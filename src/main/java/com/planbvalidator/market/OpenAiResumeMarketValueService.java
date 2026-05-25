package com.planbvalidator.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.planbvalidator.config.PlanBProperties;
import com.planbvalidator.llm.JsonResponseParser;
import com.planbvalidator.llm.PromptLoader;
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

@Service
public class OpenAiResumeMarketValueService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiResumeMarketValueService.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final PlanBProperties properties;
    private final String systemPrompt;

    @Value("${OPENAI_API_KEY:}")
    private String apiKey;

    public OpenAiResumeMarketValueService(ObjectMapper objectMapper,
                                          PlanBProperties properties,
                                          PromptLoader promptLoader) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.webClient = WebClient.builder().baseUrl(properties.openai().baseUrl()).build();
        this.systemPrompt = promptLoader.load("prompts/resume-market-value-system.txt");
    }

    public boolean isConfigured() {
        return properties.openai().enabled() && apiKey != null && !apiKey.isBlank();
    }

    public Optional<MarketValueAssessment> assess(AnalysisPipelineMemory memory) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        if (!memory.hasResume()) {
            return Optional.empty();
        }
        try {
            String userContent = objectMapper.writeValueAsString(memory.compactForResumeAnalysis());

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
            return Optional.of(parse(content));
        } catch (Exception e) {
            log.warn("openai event=resume_market_value_failed error={}", e.toString(), e);
            return Optional.empty();
        }
    }

    private MarketValueAssessment parse(String json) throws Exception {
        JsonNode n = objectMapper.readTree(json);
        return new MarketValueAssessment(
                JsonResponseParser.text(n, "credential_tier", "moderate"),
                n.path("market_value_score").asInt(50),
                JsonResponseParser.text(n, "estimated_salary_range", "unknown"),
                JsonResponseParser.text(n, "corporate_opportunity_summary", ""),
                JsonResponseParser.text(n, "plan_b_roi_summary", ""),
                n.path("opportunity_cost_risk").asInt(50),
                JsonResponseParser.stringList(objectMapper, n, "key_signals"),
                JsonResponseParser.stringList(objectMapper, n, "assumptions"),
                JsonResponseParser.stringList(objectMapper, n, "recent_employers"),
                JsonResponseParser.stringList(objectMapper, n, "recent_job_titles"),
                JsonResponseParser.stringList(objectMapper, n, "comp_search_queries")
        );
    }
}
