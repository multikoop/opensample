package com.example.opensample.kafka.api;

import com.example.opensample.kafka.service.KafkaSampleDataService;
import com.example.opensample.kafka.service.KafkaSeedAlreadyRunningException;
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

@WebMvcTest(KafkaAdminController.class)
class KafkaAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KafkaSampleDataService kafkaSampleDataService;

    @Test
    void seedShouldReturn200WhenSuccessful() throws Exception {
        doNothing().when(kafkaSampleDataService).seedSampleData();

        mockMvc.perform(post("/api/v1/admin/kafka/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void seedShouldReturn409WhenAnotherRunIsInProgress() throws Exception {
        doThrow(new KafkaSeedAlreadyRunningException()).when(kafkaSampleDataService).seedSampleData();

        mockMvc.perform(post("/api/v1/admin/kafka/seed"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Kafka seed already running"));
    }

    @Test
    void seedShouldReturn503WhenKafkaIsUnavailable() throws Exception {
        RuntimeException unavailable = new RuntimeException("kafka down");
        doThrow(unavailable).when(kafkaSampleDataService).seedSampleData();
        given(kafkaSampleDataService.isUnavailable(unavailable)).willReturn(true);

        mockMvc.perform(post("/api/v1/admin/kafka/seed"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Kafka unavailable"));
    }
}
