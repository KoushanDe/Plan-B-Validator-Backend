package com.planbvalidator.api;

import com.planbvalidator.domain.response.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class HealthController {

    @Value("${spring.application.version:1.0.0}")
    private String version;

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("healthy", version);
    }
}
