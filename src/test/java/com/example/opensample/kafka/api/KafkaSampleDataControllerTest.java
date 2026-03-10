package com.example.opensample.kafka.api;

import com.example.opensample.kafka.api.dto.KafkaSampleEventResponse;
import com.example.opensample.kafka.service.KafkaSampleDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KafkaSampleDataController.class)
class KafkaSampleDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KafkaSampleDataService kafkaSampleDataService;

    @Test
    void listEventsShouldReturnItems() throws Exception {
        given(kafkaSampleDataService.listEvents()).willReturn(List.of(
                new KafkaSampleEventResponse(
                        "sample-account-events",
                        0,
                        12L,
                        "account-1001",
                        "{\"eventType\":\"AccountFreigeschaltet\"}",
                        LocalDateTime.parse("2026-03-01T08:00:00")
                )
        ));

        mockMvc.perform(get("/api/v1/kafka/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].topic").value("sample-account-events"))
                .andExpect(jsonPath("$[0].partition").value(0))
                .andExpect(jsonPath("$[0].offset").value(12))
                .andExpect(jsonPath("$[0].key").value("account-1001"));
    }
}
