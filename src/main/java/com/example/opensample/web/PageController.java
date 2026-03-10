package com.example.opensample.web;

import com.example.opensample.api.dto.PagedResponse;
import com.example.opensample.api.dto.SampleDataResponse;
import com.example.opensample.cassandra.service.CassandraSampleDataService;
import com.example.opensample.kafka.service.KafkaSampleDataService;
import com.example.opensample.s3.service.S3SampleDataService;
import com.example.opensample.service.SampleDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PageController {

    private final SampleDataService sampleDataService;
    private final CassandraSampleDataService cassandraSampleDataService;
    private final S3SampleDataService s3SampleDataService;
    private final KafkaSampleDataService kafkaSampleDataService;

    public PageController(
            SampleDataService sampleDataService,
            CassandraSampleDataService cassandraSampleDataService,
            S3SampleDataService s3SampleDataService,
            KafkaSampleDataService kafkaSampleDataService
    ) {
        this.sampleDataService = sampleDataService;
        this.cassandraSampleDataService = cassandraSampleDataService;
        this.s3SampleDataService = s3SampleDataService;
        this.kafkaSampleDataService = kafkaSampleDataService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("activeTab", "home");
        return "index";
    }

    @GetMapping("/mariadb")
    public String mariadb(Model model) {
        model.addAttribute("activeTab", "mariadb");
        try {
            PagedResponse<SampleDataResponse> result = sampleDataService.findAll(0, 100);
            model.addAttribute("items", result.items());
        } catch (RuntimeException exception) {
            model.addAttribute("items", List.of());
            model.addAttribute("dbError", "Datenbank nicht erreichbar oder Migration noch nicht ausgefuehrt.");
        }
        return "mariadb";
    }

    @GetMapping("/streaming")
    public String streaming(Model model) {
        model.addAttribute("activeTab", "streaming");
        return "streaming";
    }

    @GetMapping("/cassandra")
    public String cassandra(Model model) {
        model.addAttribute("activeTab", "cassandra");
        try {
            model.addAttribute("items", cassandraSampleDataService.findAll());
        } catch (RuntimeException exception) {
            model.addAttribute("items", List.of());
            model.addAttribute("dbError", "Cassandra nicht erreichbar oder Seed noch nicht ausgefuehrt.");
        }
        return "cassandra";
    }

    @GetMapping("/s3")
    public String s3(Model model) {
        model.addAttribute("activeTab", "s3");
        try {
            model.addAttribute("items", s3SampleDataService.listObjects());
        } catch (RuntimeException exception) {
            model.addAttribute("items", List.of());
            model.addAttribute("dbError", "S3 nicht erreichbar oder Seed noch nicht ausgefuehrt.");
        }
        return "s3";
    }

    @GetMapping("/kafka")
    public String kafka(Model model) {
        model.addAttribute("activeTab", "kafka");
        try {
            model.addAttribute("items", kafkaSampleDataService.listEvents());
        } catch (RuntimeException exception) {
            model.addAttribute("items", List.of());
            model.addAttribute("dbError", "Kafka nicht erreichbar oder Seed noch nicht ausgefuehrt.");
        }
        return "kafka";
    }
}
