package com.example.opensample.cassandra.api;

import com.example.opensample.api.dto.MigrationRunResponse;
import com.example.opensample.cassandra.service.CassandraSampleDataService;
import com.example.opensample.cassandra.service.CassandraSeedAlreadyRunningException;
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
@RequestMapping("/api/v1/admin/cassandra")
@Tag(name = "Cassandra Admin", description = "Administrative Cassandra sample-data operations")
public class CassandraAdminController {

    private final CassandraSampleDataService cassandraSampleDataService;

    public CassandraAdminController(CassandraSampleDataService cassandraSampleDataService) {
        this.cassandraSampleDataService = cassandraSampleDataService;
    }

    @PostMapping("/seed")
    @Operation(summary = "Create Cassandra schema and seed sample data")
    @ApiResponse(responseCode = "200", description = "Seed completed")
    @ApiResponse(responseCode = "409", description = "Seed already running")
    @ApiResponse(responseCode = "503", description = "Cassandra unavailable")
    public ResponseEntity<?> seed() {
        try {
            cassandraSampleDataService.seedSampleData();
            return ResponseEntity.ok(new MigrationRunResponse(
                    "ok",
                    "Cassandra schema and sample data created successfully",
                    OffsetDateTime.now()
            ));
        } catch (CassandraSeedAlreadyRunningException exception) {
            return problem(HttpStatus.CONFLICT, "Cassandra seed already running", exception);
        } catch (RuntimeException exception) {
            HttpStatus status = cassandraSampleDataService.isUnavailable(exception)
                    ? HttpStatus.SERVICE_UNAVAILABLE
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            String title = status == HttpStatus.SERVICE_UNAVAILABLE
                    ? "Cassandra unavailable"
                    : "Cassandra seed failed";
            return problem(status, title, exception);
        }
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, Exception exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        detail.setTitle(title);
        return ResponseEntity.status(status).body(detail);
    }
}
