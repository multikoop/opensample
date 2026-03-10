package com.example.opensample.mariadb.api;

import com.example.opensample.mariadb.service.LiquibaseMigrationService;
import com.example.opensample.mariadb.service.MigrationAlreadyRunningException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;

import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatabaseAdminController.class)
class DatabaseAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LiquibaseMigrationService liquibaseMigrationService;

    @Test
    void migrateShouldReturn200WhenSuccessful() throws Exception {
        doNothing().when(liquibaseMigrationService).runMigrations();

        mockMvc.perform(post("/api/v1/admin/db/migrate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void migrateShouldReturn409WhenAnotherRunIsInProgress() throws Exception {
        doThrow(new MigrationAlreadyRunningException()).when(liquibaseMigrationService).runMigrations();

        mockMvc.perform(post("/api/v1/admin/db/migrate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Migration already running"));
    }

    @Test
    void migrateShouldReturn503WhenDatabaseIsUnavailable() throws Exception {
        doThrow(new DataAccessResourceFailureException("db down", new SQLException("connection refused")))
                .when(liquibaseMigrationService)
                .runMigrations();

        mockMvc.perform(post("/api/v1/admin/db/migrate"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Database unavailable"));
    }
}
