package com.example.opensample.api;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class HealthController {

    @GetMapping("/mt/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
