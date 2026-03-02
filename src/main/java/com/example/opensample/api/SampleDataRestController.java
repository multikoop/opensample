package com.example.opensample.api;

import com.example.opensample.api.dto.PagedResponse;
import com.example.opensample.api.dto.SampleDataResponse;
import com.example.opensample.service.SampleDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/sample-data")
@Tag(name = "Sample Data", description = "Read-only endpoints for sample data")
public class SampleDataRestController {

    private final SampleDataService service;

    public SampleDataRestController(SampleDataService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List sample data", description = "Returns paged sample data")
    public PagedResponse<SampleDataResponse> list(
            @Parameter(description = "Zero-based page number")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size between 1 and 100")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return service.findAll(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one sample data record by id")
    @ApiResponse(responseCode = "404", description = "Item not found")
    public SampleDataResponse getById(@PathVariable long id) {
        return service.findById(id);
    }
}
