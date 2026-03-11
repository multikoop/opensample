package com.example.opensample.opensearch.api;

import com.example.opensample.opensearch.api.dto.OpenSearchSampleDocumentResponse;
import com.example.opensample.opensearch.service.OpenSearchSampleDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opensearch")
@Tag(name = "OpenSearch Sample Data", description = "Read sample OpenSearch documents")
public class OpenSearchSampleDataController {

    private final OpenSearchSampleDataService openSearchSampleDataService;

    public OpenSearchSampleDataController(OpenSearchSampleDataService openSearchSampleDataService) {
        this.openSearchSampleDataService = openSearchSampleDataService;
    }

    @GetMapping("/documents")
    @Operation(summary = "List sample OpenSearch documents")
    public List<OpenSearchSampleDocumentResponse> listDocuments() {
        return openSearchSampleDataService.listDocuments();
    }
}
