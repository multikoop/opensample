package com.example.opensample.s3.api.dto;

import java.time.LocalDateTime;

public record S3SampleObjectResponse(
        String key,
        Long size,
        LocalDateTime lastModified
) {
}
