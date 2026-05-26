package com.planbvalidator.resume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.planbvalidator.config.PlanBProperties;
import com.planbvalidator.llm.JsonResponseParser;
import com.planbvalidator.llm.PromptLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class OpenAiResumeProfileParserService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiResumeProfileParserService.class);
    private static final int MAX_RESUME_CHARS = 12_000;
    private static final int DEFAULT_PARSE_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MS = 400L;

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final PlanBProperties properties;
    private final String systemPrompt;

    @Value("${OPENAI_API_KEY:}")
    private String apiKey;

    public OpenAiResumeProfileParserService(ObjectMapper objectMapper,
                                            PlanBProperties properties,
                                            PromptLoader promptLoader) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.webClient = WebClient.builder().baseUrl(properties.openai().baseUrl()).build();
        this.systemPrompt = promptLoader.load("prompts/resume-profile-system.txt");
    }

    public boolean isConfigured() {
        return properties.openai().enabled() && apiKey != null && !apiKey.isBlank();
    }

    public Optional<ResumeProfileExtraction> parse(String resumeText) {
        return parseWithRetries(resumeText, DEFAULT_PARSE_ATTEMPTS);
    }

    /**
     * Retries transient OpenAI failures (e.g. connection reset) when a resume PDF was provided.
     */
    public Optional<ResumeProfileExtraction> parseWithRetries(String resumeText, int maxAttempts) {
        if (!isConfigured() || resumeText == null || resumeText.isBlank()) {
            return Optional.empty();
        }
        int attempts = Math.max(1, maxAttempts);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            Optional<ResumeProfileExtraction> result = parseOnce(resumeText);
            if (result.isPresent()) {
                if (attempt > 1) {
                    log.info("openai event=resume_profile_recovered attempt={}/{}", attempt, attempts);
                }
                return result;
            }
            if (attempt < attempts) {
                log.warn("openai event=resume_profile_retry attempt={}/{}", attempt, attempts);
                sleepQuietly(RETRY_BACKOFF_MS * attempt);
            }
        }
        log.warn("openai event=resume_profile_exhausted attempts={}", attempts);
        return Optional.empty();
    }

    private Optional<ResumeProfileExtraction> parseOnce(String resumeText) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("resume_text", truncate(resumeText, MAX_RESUME_CHARS));

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", properties.openai().model());
            requestBody.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));
            var messages = requestBody.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject()
                    .put("role", "user")
                    .put("content", objectMapper.writeValueAsString(payload));

            String response = webClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(properties.openai().timeoutMs()))
                    .block();

            if (response == null || response.isBlank()) {
                return Optional.empty();
            }

            String content = objectMapper.readTree(response)
                    .path("choices").path(0).path("message").path("content").asText();
            return Optional.of(parseContent(content));
        } catch (Exception e) {
            log.warn("openai event=resume_profile_failed error={}", e.toString(), e);
            return Optional.empty();
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ResumeProfileExtraction parseContent(String json) throws Exception {
        JsonNode n = objectMapper.readTree(json);
        Double years = n.path("years_experience").isNumber() ? n.path("years_experience").asDouble() : null;
        if (years != null && years < 0) {
            years = null;
        }
        return new ResumeProfileExtraction(
                nullIfBlank(JsonResponseParser.text(n, "current_profession", "")),
                nullIfBlank(JsonResponseParser.text(n, "industry", "")),
                years,
                nullIfBlank(JsonResponseParser.text(n, "country", "")),
                nullIfBlank(JsonResponseParser.text(n, "city", "")),
                nullIfBlank(JsonResponseParser.text(n, "target_country", "")),
                nullIfBlank(JsonResponseParser.text(n, "target_city", "")),
                JsonResponseParser.stringList(objectMapper, n, "assumptions")
        );
    }

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
