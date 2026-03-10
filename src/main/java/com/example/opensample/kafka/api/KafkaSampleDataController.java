package com.example.opensample.kafka.api;

import com.example.opensample.kafka.api.dto.KafkaSampleEventResponse;
import com.example.opensample.kafka.service.KafkaSampleDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kafka")
@Tag(name = "Kafka Sample Data", description = "Read sample Kafka events")
public class KafkaSampleDataController {

    private final KafkaSampleDataService kafkaSampleDataService;

    public KafkaSampleDataController(KafkaSampleDataService kafkaSampleDataService) {
        this.kafkaSampleDataService = kafkaSampleDataService;
    }

    @GetMapping("/events")
    @Operation(summary = "List sample Kafka events")
    public List<KafkaSampleEventResponse> listEvents() {
        return kafkaSampleDataService.listEvents();
    }
}
