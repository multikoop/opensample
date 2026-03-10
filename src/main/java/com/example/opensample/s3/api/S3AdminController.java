package com.example.opensample.s3.api;

import com.example.opensample.api.dto.MigrationRunResponse;
import com.example.opensample.s3.service.S3SampleDataService;
import com.example.opensample.s3.service.S3SeedAlreadyRunningException;
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
@RequestMapping("/api/v1/admin/s3")
@Tag(name = "S3 Admin", description = "Administrative S3 sample-data operations")
public class S3AdminController {

    private final S3SampleDataService s3SampleDataService;

    public S3AdminController(S3SampleDataService s3SampleDataService) {
        this.s3SampleDataService = s3SampleDataService;
    }

    @PostMapping("/seed")
    @Operation(summary = "Create sample S3 bucket and upload example files")
    @ApiResponse(responseCode = "200", description = "Seed completed")
    @ApiResponse(responseCode = "409", description = "Seed already running")
    @ApiResponse(responseCode = "503", description = "S3 unavailable")
    public ResponseEntity<?> seed() {
        try {
            s3SampleDataService.seedSampleData();
            return ResponseEntity.ok(new MigrationRunResponse(
                    "ok",
                    "S3 sample bucket and files created successfully",
                    OffsetDateTime.now()
            ));
        } catch (S3SeedAlreadyRunningException exception) {
            return problem(HttpStatus.CONFLICT, "S3 seed already running", exception);
        } catch (RuntimeException exception) {
            HttpStatus status = s3SampleDataService.isUnavailable(exception)
                    ? HttpStatus.SERVICE_UNAVAILABLE
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            String title = status == HttpStatus.SERVICE_UNAVAILABLE
                    ? "S3 unavailable"
                    : "S3 seed failed";
            return problem(status, title, exception);
        }
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, Exception exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        detail.setTitle(title);
        return ResponseEntity.status(status).body(detail);
    }
}
