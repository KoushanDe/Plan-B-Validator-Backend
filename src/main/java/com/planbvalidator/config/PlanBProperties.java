package com.planbvalidator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "planb")
public record PlanBProperties(
        Research research,
        OpenAi openai,
        Gemini gemini,
        RateLimit rateLimit,
        String disclaimer
) {
    public record Research(boolean enabled, String provider, int timeoutMs, String baseUrl, String model) {}

    public record OpenAi(boolean enabled, String model, int timeoutMs, String baseUrl) {}

    public record Gemini(boolean enabled, String model, int timeoutMs, String baseUrl) {}

    /**
     * Applies to {@code POST /v1/analyze} only.
     */
    public record RateLimit(
            int analyzePerUserRequests,
            int analyzePerUserWindowMinutes,
            int analyzeGlobalRequestsPerMinute
    ) {
        public RateLimit {
            if (analyzePerUserRequests <= 0) {
                analyzePerUserRequests = 1;
            }
            if (analyzePerUserWindowMinutes <= 0) {
                analyzePerUserWindowMinutes = 5;
            }
            if (analyzeGlobalRequestsPerMinute <= 0) {
                analyzeGlobalRequestsPerMinute = 5;
            }
        }
    }
}
