package com.example.opensample.kafka.api.dto;

import java.time.LocalDateTime;

public record KafkaSampleEventResponse(
        String topic,
        int partition,
        long offset,
        String key,
        String value,
        LocalDateTime timestamp
) {
}
