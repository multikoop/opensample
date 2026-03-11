package com.example.opensample.opensearch.api.dto;

import java.time.LocalDateTime;

public record OpenSearchSampleDocumentResponse(
        String index,
        String id,
        Double score,
        String title,
        String sourceJson,
        LocalDateTime timestamp
) {
}
