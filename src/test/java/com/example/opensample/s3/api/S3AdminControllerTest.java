package com.example.opensample.s3.api;

import com.example.opensample.s3.service.S3SampleDataService;
import com.example.opensample.s3.service.S3SeedAlreadyRunningException;
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

@WebMvcTest(S3AdminController.class)
class S3AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private S3SampleDataService s3SampleDataService;

    @Test
    void seedShouldReturn200WhenSuccessful() throws Exception {
        doNothing().when(s3SampleDataService).seedSampleData();

        mockMvc.perform(post("/api/v1/admin/s3/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void seedShouldReturn409WhenAnotherRunIsInProgress() throws Exception {
        doThrow(new S3SeedAlreadyRunningException()).when(s3SampleDataService).seedSampleData();

        mockMvc.perform(post("/api/v1/admin/s3/seed"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("S3 seed already running"));
    }

    @Test
    void seedShouldReturn503WhenS3IsUnavailable() throws Exception {
        RuntimeException unavailable = new RuntimeException("s3 down");
        doThrow(unavailable).when(s3SampleDataService).seedSampleData();
        given(s3SampleDataService.isUnavailable(unavailable)).willReturn(true);

        mockMvc.perform(post("/api/v1/admin/s3/seed"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("S3 unavailable"));
    }
}
