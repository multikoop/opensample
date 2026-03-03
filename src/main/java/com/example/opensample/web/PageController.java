package com.example.opensample.web;

import com.example.opensample.api.dto.PagedResponse;
import com.example.opensample.api.dto.SampleDataResponse;
import com.example.opensample.service.SampleDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PageController {

    private final SampleDataService sampleDataService;

    public PageController(SampleDataService sampleDataService) {
        this.sampleDataService = sampleDataService;
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
}
