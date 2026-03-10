package com.example.opensample.s3.api;

import com.example.opensample.s3.api.dto.S3SampleObjectResponse;
import com.example.opensample.s3.service.S3DownloadedObject;
import com.example.opensample.s3.service.S3SampleDataService;
import com.example.opensample.s3.service.S3SampleObjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(S3SampleDataController.class)
@Import(S3RestExceptionHandler.class)
class S3SampleDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private S3SampleDataService s3SampleDataService;

    @Test
    void listObjectsShouldReturnItems() throws Exception {
        given(s3SampleDataService.listObjects()).willReturn(List.of(
                new S3SampleObjectResponse("sample-note.txt", 128L, LocalDateTime.parse("2026-03-01T08:00:00"))
        ));

        mockMvc.perform(get("/api/v1/s3/objects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("sample-note.txt"))
                .andExpect(jsonPath("$[0].size").value(128));
    }

    @Test
    void downloadShouldReturnAttachment() throws Exception {
        byte[] body = "hello s3".getBytes(StandardCharsets.UTF_8);
        given(s3SampleDataService.downloadObject("sample-note.txt"))
                .willReturn(new S3DownloadedObject("sample-note.txt", "text/plain", body));

        mockMvc.perform(get("/api/v1/s3/objects/download").param("key", "sample-note.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(content().bytes(body));
    }

    @Test
    void downloadShouldReturn404WhenObjectIsMissing() throws Exception {
        given(s3SampleDataService.downloadObject("missing.pdf"))
                .willThrow(new S3SampleObjectNotFoundException("missing.pdf"));

        mockMvc.perform(get("/api/v1/s3/objects/download").param("key", "missing.pdf"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("S3 object not found"));
    }
}
