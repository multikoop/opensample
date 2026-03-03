package com.example.opensample.api.dto;

import java.time.OffsetDateTime;

public record MigrationRunResponse(
        String status,
        String message,
        OffsetDateTime triggeredAt
) {
}
