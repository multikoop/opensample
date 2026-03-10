package com.example.opensample.kafka.service;

import com.example.opensample.kafka.api.dto.KafkaSampleEventResponse;
import com.example.opensample.kafka.config.KafkaSampleProperties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.DisconnectException;
import org.apache.kafka.common.errors.NetworkException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class KafkaSampleDataService {

    private static final List<SeedMessage> DEFAULT_MESSAGES = List.of(
            new SeedMessage("account-1001", "{\"eventType\":\"AccountFreigeschaltet\",\"accountId\":\"1001\",\"reason\":\"KYC_OK\"}"),
            new SeedMessage("account-1002", "{\"eventType\":\"AccountFreigeschaltet\",\"accountId\":\"1002\",\"reason\":\"MANUAL_REVIEW_OK\"}"),
            new SeedMessage("account-1003", "{\"eventType\":\"AccountFreigeschaltet\",\"accountId\":\"1003\",\"reason\":\"AUTO_APPROVAL\"}")
    );

    private final KafkaSampleProperties properties;
    private final ReentrantLock seedLock = new ReentrantLock();

    public KafkaSampleDataService(KafkaSampleProperties properties) {
        this.properties = properties;
    }

    public void seedSampleData() {
        if (!seedLock.tryLock()) {
            throw new KafkaSeedAlreadyRunningException();
        }

        String topic = validTopic(properties.getTopic());

        try (AdminClient adminClient = AdminClient.create(adminClientProperties());
             KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties())) {
            ensureTopicExists(adminClient, topic);
            produceMessages(producer, topic);
        } finally {
            seedLock.unlock();
        }
    }

    public List<KafkaSampleEventResponse> listEvents() {
        String topic = validTopic(properties.getTopic());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties())) {
            List<PartitionInfo> partitionInfos = consumer.partitionsFor(
                    topic,
                    Duration.ofMillis(validRequestTimeout(properties.getRequestTimeoutMs()))
            );
            if (partitionInfos == null || partitionInfos.isEmpty()) {
                return List.of();
            }

            List<TopicPartition> partitions = partitionInfos.stream()
                    .map(info -> new TopicPartition(info.topic(), info.partition()))
                    .toList();

            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);

            List<KafkaSampleEventResponse> events = new ArrayList<>();
            int emptyPollCount = 0;
            int maxEvents = validMaxEvents(properties.getMaxEvents());
            long pollTimeout = validPollTimeout(properties.getPollTimeoutMs());

            while (emptyPollCount < 2 && events.size() < maxEvents) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(pollTimeout));
                if (records.isEmpty()) {
                    emptyPollCount++;
                    continue;
                }
                emptyPollCount = 0;

                for (ConsumerRecord<String, String> record : records) {
                    events.add(toResponse(record));
                    if (events.size() >= maxEvents) {
                        break;
                    }
                }
            }

            events.sort(Comparator
                    .comparingInt(KafkaSampleEventResponse::partition)
                    .thenComparingLong(KafkaSampleEventResponse::offset));
            return events;
        } catch (UnknownTopicOrPartitionException exception) {
            return List.of();
        }
    }

    public boolean isUnavailable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof TimeoutException
                    || current instanceof DisconnectException
                    || current instanceof NetworkException
                    || current instanceof ConnectException
                    || current instanceof UnknownHostException) {
                return true;
            }

            if (current instanceof KafkaException kafkaException
                    && kafkaException.getMessage() != null
                    && kafkaException.getMessage().toLowerCase(Locale.ROOT).contains("bootstrap")) {
                return true;
            }

            current = current.getCause();
        }
        return false;
    }

    private void ensureTopicExists(AdminClient adminClient, String topic) {
        try {
            Set<String> existingTopics = adminClient.listTopics().names()
                    .get(validRequestTimeout(properties.getRequestTimeoutMs()), TimeUnit.MILLISECONDS);
            if (existingTopics.contains(topic)) {
                return;
            }

            NewTopic newTopic = new NewTopic(topic, validPartitions(properties.getPartitions()), validReplicationFactor(properties.getReplicationFactor()));
            adminClient.createTopics(List.of(newTopic)).all()
                    .get(validRequestTimeout(properties.getRequestTimeoutMs()), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka topic creation interrupted", exception);
        } catch (java.util.concurrent.TimeoutException exception) {
            throw new IllegalStateException("Kafka topic creation timed out", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof TopicExistsException) {
                return;
            }
            throw new IllegalStateException("Kafka topic creation failed", cause);
        }
    }

    private void produceMessages(KafkaProducer<String, String> producer, String topic) {
        for (SeedMessage message : DEFAULT_MESSAGES) {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, message.key(), message.value());
            try {
                producer.send(record).get(validRequestTimeout(properties.getRequestTimeoutMs()), TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Kafka seed interrupted", exception);
            } catch (java.util.concurrent.TimeoutException exception) {
                throw new IllegalStateException("Kafka seed timed out", exception);
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                throw new IllegalStateException("Kafka seed failed", cause == null ? exception : cause);
            }
        }
        producer.flush();
    }

    private Properties adminClientProperties() {
        Properties props = baseClientProperties();
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, Math.toIntExact(validRequestTimeout(properties.getRequestTimeoutMs())));
        return props;
    }

    private Properties producerProperties() {
        Properties props = baseClientProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return props;
    }

    private Properties consumerProperties() {
        Properties props = baseClientProperties();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "opensample-kafka-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return props;
    }

    private Properties baseClientProperties() {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, validBootstrapServers(properties.getBootstrapServers()));
        props.put(CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, Math.toIntExact(validRequestTimeout(properties.getRequestTimeoutMs())));
        return props;
    }

    private KafkaSampleEventResponse toResponse(ConsumerRecord<String, String> record) {
        return new KafkaSampleEventResponse(
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value(),
                toUtcDateTime(record.timestamp())
        );
    }

    private LocalDateTime toUtcDateTime(long epochMillis) {
        if (epochMillis < 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }

    private String validBootstrapServers(String bootstrapServers) {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            throw new IllegalArgumentException("Kafka bootstrapServers must not be blank");
        }
        return bootstrapServers.trim();
    }

    private String validTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Kafka topic must not be blank");
        }

        String normalized = topic.trim();
        if (!normalized.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException(
                    "Invalid Kafka topic '" + topic + "'. Allowed: letters, numbers, dots, dashes, underscore"
            );
        }
        return normalized;
    }

    private int validPartitions(int partitions) {
        if (partitions < 1) {
            throw new IllegalArgumentException("Kafka partitions must be >= 1");
        }
        return partitions;
    }

    private short validReplicationFactor(short replicationFactor) {
        if (replicationFactor < 1) {
            throw new IllegalArgumentException("Kafka replicationFactor must be >= 1");
        }
        return replicationFactor;
    }

    private long validRequestTimeout(long requestTimeoutMs) {
        if (requestTimeoutMs < 100) {
            throw new IllegalArgumentException("Kafka requestTimeoutMs must be >= 100");
        }
        return requestTimeoutMs;
    }

    private long validPollTimeout(long pollTimeoutMs) {
        if (pollTimeoutMs < 50) {
            throw new IllegalArgumentException("Kafka pollTimeoutMs must be >= 50");
        }
        return pollTimeoutMs;
    }

    private int validMaxEvents(int maxEvents) {
        if (maxEvents < 1) {
            throw new IllegalArgumentException("Kafka maxEvents must be >= 1");
        }
        return maxEvents;
    }

    private record SeedMessage(String key, String value) {
    }
}
