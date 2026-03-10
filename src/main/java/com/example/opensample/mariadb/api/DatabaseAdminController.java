package com.example.opensample.mariadb.api;

import com.example.opensample.api.dto.MigrationRunResponse;
import com.example.opensample.mariadb.service.LiquibaseMigrationService;
import com.example.opensample.mariadb.service.MigrationAlreadyRunningException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import liquibase.exception.LiquibaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.ConnectException;
import java.sql.SQLException;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/admin/db")
@Tag(name = "Database Admin", description = "Administrative database operations")
public class DatabaseAdminController {

    private final LiquibaseMigrationService liquibaseMigrationService;

    public DatabaseAdminController(LiquibaseMigrationService liquibaseMigrationService) {
        this.liquibaseMigrationService = liquibaseMigrationService;
    }

    @PostMapping("/migrate")
    @Operation(summary = "Run Liquibase migration manually")
    @ApiResponse(responseCode = "200", description = "Migration completed")
    @ApiResponse(responseCode = "409", description = "Migration already running")
    @ApiResponse(responseCode = "503", description = "Database unavailable")
    public ResponseEntity<?> migrate() {
        try {
            liquibaseMigrationService.runMigrations();
            return ResponseEntity.ok(new MigrationRunResponse(
                    "ok",
                    "Liquibase migration completed successfully",
                    OffsetDateTime.now()
            ));
        } catch (MigrationAlreadyRunningException exception) {
            return problem(HttpStatus.CONFLICT, "Migration already running", exception);
        } catch (LiquibaseException | RuntimeException exception) {
            HttpStatus status = isDatabaseUnavailable(exception)
                    ? HttpStatus.SERVICE_UNAVAILABLE
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            String title = status == HttpStatus.SERVICE_UNAVAILABLE
                    ? "Database unavailable"
                    : "Migration failed";
            return problem(status, title, exception);
        }
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, Exception exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        detail.setTitle(title);
        return ResponseEntity.status(status).body(detail);
    }

    private boolean isDatabaseUnavailable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException) {
                return true;
            }
            if (current instanceof ConnectException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
