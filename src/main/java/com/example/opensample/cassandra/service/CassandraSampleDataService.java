package com.example.opensample.cassandra.service;

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.example.opensample.cassandra.api.dto.CassandraSampleDataResponse;
import com.example.opensample.cassandra.config.CassandraSampleProperties;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CassandraSampleDataService {

    private static final List<SeedRow> DEFAULT_ROWS = List.of(
            new SeedRow(1L, "Sample row 1", "Initial sample record", LocalDateTime.parse("2026-03-01T08:00:00")),
            new SeedRow(2L, "Sample row 2", "Sample record for API checks", LocalDateTime.parse("2026-03-01T09:00:00")),
            new SeedRow(3L, "Sample row 3", "Used in thymeleaf table output", LocalDateTime.parse("2026-03-01T10:00:00")),
            new SeedRow(4L, "Sample row 4", "Reference entry for tests", LocalDateTime.parse("2026-03-01T11:00:00")),
            new SeedRow(5L, "Sample row 5", "Final seeded row", LocalDateTime.parse("2026-03-01T12:00:00"))
    );

    private final CassandraSampleProperties properties;
    private final ReentrantLock seedLock = new ReentrantLock();

    public CassandraSampleDataService(CassandraSampleProperties properties) {
        this.properties = properties;
    }

    public void seedSampleData() {
        if (!seedLock.tryLock()) {
            throw new CassandraSeedAlreadyRunningException();
        }

        try (CqlSession session = openSession()) {
            session.execute(createKeyspaceStatement());
            session.execute(createTableStatement());

            PreparedStatement insertStatement = session.prepare(
                    "INSERT INTO " + tableName() + " (id, name, description, created_at) VALUES (?, ?, ?, ?)"
            );

            for (SeedRow row : DEFAULT_ROWS) {
                session.execute(insertStatement.bind(
                        row.id(),
                        row.name(),
                        row.description(),
                        row.createdAt().toInstant(ZoneOffset.UTC)
                ));
            }
        } finally {
            seedLock.unlock();
        }
    }

    public List<CassandraSampleDataResponse> findAll() {
        try (CqlSession session = openSession()) {
            ResultSet resultSet = session.execute(
                    "SELECT id, name, description, created_at FROM " + tableName()
            );

            List<CassandraSampleDataResponse> rows = new ArrayList<>();
            for (Row row : resultSet) {
                rows.add(toResponse(row));
            }

            rows.sort(Comparator.comparing(CassandraSampleDataResponse::id));
            return rows;
        }
    }

    private CqlSession openSession() {
        DriverConfigLoader configLoader = DriverConfigLoader.programmaticBuilder()
                .withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, Duration.ofMillis(properties.getConnectTimeoutMs()))
                .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofMillis(properties.getRequestTimeoutMs()))
                .build();

        CqlSession.CqlSessionBuilder sessionBuilder = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(properties.getHost(), properties.getPort()))
                .withLocalDatacenter(validDatacenter(properties.getDatacenter()))
                .withConfigLoader(configLoader);

        if (hasText(properties.getUsername()) && hasText(properties.getPassword())) {
            sessionBuilder.withAuthCredentials(properties.getUsername().trim(), properties.getPassword());
        }

        return sessionBuilder.build();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String createKeyspaceStatement() {
        return "CREATE KEYSPACE IF NOT EXISTS " + keyspaceName()
               + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}";
    }

    private String createTableStatement() {
        return "CREATE TABLE IF NOT EXISTS " + tableName() + " ("
               + "id BIGINT PRIMARY KEY, "
               + "name TEXT, "
               + "description TEXT, "
               + "created_at TIMESTAMP"
               + ")";
    }

    private CassandraSampleDataResponse toResponse(Row row) {
        Instant createdAt = row.getInstant("created_at");
        return new CassandraSampleDataResponse(
                row.getLong("id"),
                row.getString("name"),
                row.getString("description"),
                createdAt == null ? null : LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC)
        );
    }

    private String keyspaceName() {
        return validKeyspace(properties.getKeyspace());
    }

    private String tableName() {
        return keyspaceName() + ".sample_data";
    }

    private String validKeyspace(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Cassandra keyspace must not be blank");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException(
                    "Invalid Cassandra keyspace '" + value + "'. Allowed: a-z, 0-9, underscore"
            );
        }
        return normalized;
    }

    private String validDatacenter(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Cassandra datacenter must not be blank");
        }

        String trimmed = value.trim();
        if (!trimmed.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Invalid Cassandra datacenter '" + value + "'. Allowed: letters, numbers, underscore, dash"
            );
        }
        return trimmed;
    }

    public boolean isUnavailable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof AllNodesFailedException) {
                return true;
            }
            if (current instanceof java.net.ConnectException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record SeedRow(long id, String name, String description, LocalDateTime createdAt) {
    }
}
