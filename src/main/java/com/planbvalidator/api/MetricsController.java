package com.planbvalidator.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1")
public class MetricsController {

    @GetMapping("/metrics")
    public Map<String, String> metrics() {
        return Map.of("status", "not_implemented");
    }
}
