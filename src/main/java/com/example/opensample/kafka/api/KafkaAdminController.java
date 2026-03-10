package com.example.opensample.kafka.api;

import com.example.opensample.api.dto.MigrationRunResponse;
import com.example.opensample.kafka.service.KafkaSampleDataService;
import com.example.opensample.kafka.service.KafkaSeedAlreadyRunningException;
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
@RequestMapping("/api/v1/admin/kafka")
@Tag(name = "Kafka Admin", description = "Administrative Kafka sample-data operations")
public class KafkaAdminController {

    private final KafkaSampleDataService kafkaSampleDataService;

    public KafkaAdminController(KafkaSampleDataService kafkaSampleDataService) {
        this.kafkaSampleDataService = kafkaSampleDataService;
    }

    @PostMapping("/seed")
    @Operation(summary = "Create Kafka topic and publish sample events")
    @ApiResponse(responseCode = "200", description = "Seed completed")
    @ApiResponse(responseCode = "409", description = "Seed already running")
    @ApiResponse(responseCode = "503", description = "Kafka unavailable")
    public ResponseEntity<?> seed() {
        try {
            kafkaSampleDataService.seedSampleData();
            return ResponseEntity.ok(new MigrationRunResponse(
                    "ok",
                    "Kafka topic and sample events created successfully",
                    OffsetDateTime.now()
            ));
        } catch (KafkaSeedAlreadyRunningException exception) {
            return problem(HttpStatus.CONFLICT, "Kafka seed already running", exception);
        } catch (RuntimeException exception) {
            HttpStatus status = kafkaSampleDataService.isUnavailable(exception)
                    ? HttpStatus.SERVICE_UNAVAILABLE
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            String title = status == HttpStatus.SERVICE_UNAVAILABLE
                    ? "Kafka unavailable"
                    : "Kafka seed failed";
            return problem(status, title, exception);
        }
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, Exception exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        detail.setTitle(title);
        return ResponseEntity.status(status).body(detail);
    }
}
