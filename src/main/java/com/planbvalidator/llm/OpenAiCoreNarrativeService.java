package com.planbvalidator.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.planbvalidator.config.PlanBProperties;
import com.planbvalidator.pipeline.AnalysisPipelineMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Optional;

/**
 * OpenAI: structured core narrative using compact pipeline memory (not full request payload).
 */
@Service
public class OpenAiCoreNarrativeService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCoreNarrativeService.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final PlanBProperties properties;
    private final String systemPrompt;

    @Value("${OPENAI_API_KEY:}")
    private String apiKey;

    public OpenAiCoreNarrativeService(ObjectMapper objectMapper,
                                      PlanBProperties properties,
                                      PromptLoader promptLoader) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.webClient = WebClient.builder().baseUrl(properties.openai().baseUrl()).build();
        this.systemPrompt = promptLoader.load("prompts/openai-core-system.txt");
    }

    public boolean isConfigured() {
        return properties.openai().enabled() && apiKey != null && !apiKey.isBlank();
    }

    public Optional<CoreNarrative> generateCore(AnalysisPipelineMemory memory) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            String userContent = objectMapper.writeValueAsString(memory.compactForCoreNarrative());

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", properties.openai().model());
            requestBody.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));
            var messages = requestBody.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userContent);

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
            return Optional.of(parseCore(content));
        } catch (Exception e) {
            log.warn("openai event=core_narrative_failed error={}", e.toString(), e);
            return Optional.empty();
        }
    }

    private CoreNarrative parseCore(String json) throws Exception {
        JsonNode n = objectMapper.readTree(json);
        return new CoreNarrative(
                JsonResponseParser.text(n, "recommendation_summary", ""),
                JsonResponseParser.stringList(objectMapper, n, "major_reasons"),
                JsonResponseParser.stringList(objectMapper, n, "red_flags"),
                JsonResponseParser.stringList(objectMapper, n, "next_steps"),
                JsonResponseParser.stringList(objectMapper, n, "assumptions"),
                JsonResponseParser.stringList(objectMapper, n, "data_gaps")
        );
    }
}
