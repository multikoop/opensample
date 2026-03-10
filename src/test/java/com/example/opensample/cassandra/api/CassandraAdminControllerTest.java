package com.example.opensample.cassandra.api;

import com.example.opensample.cassandra.service.CassandraSampleDataService;
import com.example.opensample.cassandra.service.CassandraSeedAlreadyRunningException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.ConnectException;

import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CassandraAdminController.class)
class CassandraAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CassandraSampleDataService cassandraSampleDataService;

    @Test
    void seedShouldReturn200WhenSuccessful() throws Exception {
        doNothing().when(cassandraSampleDataService).seedSampleData();

        mockMvc.perform(post("/api/v1/admin/cassandra/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void seedShouldReturn409WhenAnotherRunIsInProgress() throws Exception {
        doThrow(new CassandraSeedAlreadyRunningException()).when(cassandraSampleDataService).seedSampleData();

        mockMvc.perform(post("/api/v1/admin/cassandra/seed"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cassandra seed already running"));
    }

    @Test
    void seedShouldReturn503WhenCassandraIsUnavailable() throws Exception {
        RuntimeException unavailable = new RuntimeException("cassandra down", new ConnectException("connection refused"));
        doThrow(unavailable).when(cassandraSampleDataService).seedSampleData();
        given(cassandraSampleDataService.isUnavailable(unavailable)).willReturn(true);

        mockMvc.perform(post("/api/v1/admin/cassandra/seed"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Cassandra unavailable"));
    }
}
