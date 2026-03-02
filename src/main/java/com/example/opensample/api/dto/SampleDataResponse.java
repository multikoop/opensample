package com.example.opensample.api.dto;

import java.time.LocalDateTime;

public record SampleDataResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt
) {
}
