package com.planbvalidator.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.planbvalidator.config.PlanBProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Optional;

/**
 * Gemini: deep fields synthesized from web research + OpenAI core + resume market value + scores.
 */
@Service
public class GeminiDeepNarrativeService {

    private static final Logger log = LoggerFactory.getLogger(GeminiDeepNarrativeService.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final PlanBProperties properties;
    private final String systemPrompt;

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    public GeminiDeepNarrativeService(ObjectMapper objectMapper,
                                      PlanBProperties properties,
                                      PromptLoader promptLoader) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.webClient = WebClient.builder().baseUrl(properties.gemini().baseUrl()).build();
        this.systemPrompt = promptLoader.load("prompts/gemini-deep-system.txt");
    }

    public boolean isConfigured() {
        return properties.gemini().enabled() && apiKey != null && !apiKey.isBlank();
    }

    public Optional<DeepNarrative> generateDeep(GeminiDeepContext context) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.set("deterministic_assessment", objectMapper.valueToTree(context.memory().compactForCoreNarrative()));

            if (context.internetResearch() != null) {
                payload.set("internet_research",
                        objectMapper.valueToTree(context.internetResearch().toCompactMap()));
            } else {
                payload.putNull("internet_research");
            }

            if (context.marketValue() != null) {
                payload.set("resume_market_value", objectMapper.valueToTree(context.marketValue().toCompactMap()));
            } else {
                payload.putNull("resume_market_value");
            }

            payload.set("openai_core_narrative", objectMapper.valueToTree(context.openAiCoreNarrative()));
            payload.put("openai_core_from_provider", context.openAiCoreFromProvider());

            String userText = systemPrompt + "\n\n---\n\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            ObjectNode body = objectMapper.createObjectNode();
            body.putArray("contents")
                    .addObject()
                    .putArray("parts")
                    .addObject()
                    .put("text", userText);
            body.putObject("generationConfig").put("responseMimeType", "application/json");

            String model = properties.gemini().model();
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(properties.gemini().timeoutMs()))
                    .block();

            if (response == null || response.isBlank()) {
                return Optional.empty();
            }

            String content = objectMapper.readTree(response)
                    .path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
            return Optional.of(parseDeep(content));
        } catch (Exception e) {
            log.warn("gemini event=deep_narrative_failed error={}", e.toString(), e);
            return Optional.empty();
        }
    }

    private DeepNarrative parseDeep(String json) throws Exception {
        JsonNode n = objectMapper.readTree(json);
        return new DeepNarrative(
                JsonResponseParser.text(n, "personality_summary", ""),
                JsonResponseParser.text(n, "expected_failure_mode", ""),
                JsonResponseParser.text(n, "safest_next_move", ""),
                JsonResponseParser.text(n, "suggested_fallback_plan", "")
        );
    }
}
