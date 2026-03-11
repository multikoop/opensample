package com.example.opensample.opensearch.api;

import com.example.opensample.opensearch.api.dto.OpenSearchSampleDocumentResponse;
import com.example.opensample.opensearch.service.OpenSearchSampleDataService;
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

@WebMvcTest(OpenSearchSampleDataController.class)
class OpenSearchSampleDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenSearchSampleDataService openSearchSampleDataService;

    @Test
    void listDocumentsShouldReturnItems() throws Exception {
        given(openSearchSampleDataService.listDocuments()).willReturn(List.of(
                new OpenSearchSampleDocumentResponse(
                        "sample-index",
                        "stellenangebot-1",
                        1.0,
                        "Java Entwickler (m/w/d)",
                        "{\"title\":\"Java Entwickler (m/w/d)\"}",
                        LocalDateTime.parse("2026-03-01T08:00:00")
                )
        ));

        mockMvc.perform(get("/api/v1/opensearch/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].index").value("sample-index"))
                .andExpect(jsonPath("$[0].id").value("stellenangebot-1"))
                .andExpect(jsonPath("$[0].title").value("Java Entwickler (m/w/d)"));
    }
}
