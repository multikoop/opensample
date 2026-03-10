package com.example.opensample.s3.api;

import com.example.opensample.s3.api.dto.S3SampleObjectResponse;
import com.example.opensample.s3.service.S3DownloadedObject;
import com.example.opensample.s3.service.S3SampleDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/s3")
@Tag(name = "S3 Sample Data", description = "Read and download sample S3 files")
public class S3SampleDataController {

    private final S3SampleDataService s3SampleDataService;

    public S3SampleDataController(S3SampleDataService s3SampleDataService) {
        this.s3SampleDataService = s3SampleDataService;
    }

    @GetMapping("/objects")
    @Operation(summary = "List sample S3 objects")
    public List<S3SampleObjectResponse> listObjects() {
        return s3SampleDataService.listObjects();
    }

    @GetMapping("/objects/download")
    @Operation(summary = "Download one sample S3 object by key")
    @ApiResponse(responseCode = "404", description = "Object not found")
    public ResponseEntity<byte[]> download(
            @Parameter(description = "S3 object key")
            @RequestParam String key
    ) {
        S3DownloadedObject object = s3SampleDataService.downloadObject(key);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            mediaType = MediaType.parseMediaType(object.contentType());
        } catch (InvalidMediaTypeException ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(object.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType)
                .body(object.content());
    }
}
