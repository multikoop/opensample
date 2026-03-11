package com.example.opensample.opensearch.api;

import com.example.opensample.api.dto.MigrationRunResponse;
import com.example.opensample.opensearch.service.OpenSearchSampleDataService;
import com.example.opensample.opensearch.service.OpenSearchSeedAlreadyRunningException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/admin/opensearch")
@Tag(name = "OpenSearch Admin", description = "Administrative OpenSearch sample-data operations")
public class OpenSearchAdminController {

    private final OpenSearchSampleDataService openSearchSampleDataService;

    public OpenSearchAdminController(OpenSearchSampleDataService openSearchSampleDataService) {
        this.openSearchSampleDataService = openSearchSampleDataService;
    }

    @PostMapping("/seed")
    @Operation(summary = "Create OpenSearch index and publish sample documents")
    @ApiResponse(responseCode = "200", description = "Seed completed")
    @ApiResponse(responseCode = "409", description = "Seed already running")
    @ApiResponse(responseCode = "503", description = "OpenSearch unavailable")
    public ResponseEntity<?> seed() {
        try {
            openSearchSampleDataService.seedSampleData();
            return ResponseEntity.ok(new MigrationRunResponse(
                    "ok",
                    "OpenSearch index and sample documents created successfully",
                    OffsetDateTime.now()
            ));
        } catch (OpenSearchSeedAlreadyRunningException exception) {
            return problem(HttpStatus.CONFLICT, "OpenSearch seed already running", exception);
        } catch (RuntimeException exception) {
            HttpStatus status = openSearchSampleDataService.isUnavailable(exception)
                    ? HttpStatus.SERVICE_UNAVAILABLE
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            String title = status == HttpStatus.SERVICE_UNAVAILABLE
                    ? "OpenSearch unavailable"
                    : "OpenSearch seed failed";
            return problem(status, title, exception);
        }
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, Exception exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        detail.setTitle(title);
        return ResponseEntity.status(status).body(detail);
    }
}
