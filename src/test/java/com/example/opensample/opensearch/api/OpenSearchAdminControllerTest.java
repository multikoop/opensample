package com.example.opensample.opensearch.api;

import com.example.opensample.opensearch.service.OpenSearchSampleDataService;
import com.example.opensample.opensearch.service.OpenSearchSeedAlreadyRunningException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OpenSearchAdminController.class)
class OpenSearchAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenSearchSampleDataService openSearchSampleDataService;

    @Test
    void seedShouldReturn200WhenSuccessful() throws Exception {
        doNothing().when(openSearchSampleDataService).seedSampleData();

        mockMvc.perform(post("/api/v1/admin/opensearch/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void seedShouldReturn409WhenAnotherRunIsInProgress() throws Exception {
        doThrow(new OpenSearchSeedAlreadyRunningException()).when(openSearchSampleDataService).seedSampleData();

        mockMvc.perform(post("/api/v1/admin/opensearch/seed"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("OpenSearch seed already running"));
    }

    @Test
    void seedShouldReturn503WhenOpenSearchIsUnavailable() throws Exception {
        RuntimeException unavailable = new RuntimeException("opensearch down");
        doThrow(unavailable).when(openSearchSampleDataService).seedSampleData();
        given(openSearchSampleDataService.isUnavailable(unavailable)).willReturn(true);

        mockMvc.perform(post("/api/v1/admin/opensearch/seed"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("OpenSearch unavailable"));
    }
}
