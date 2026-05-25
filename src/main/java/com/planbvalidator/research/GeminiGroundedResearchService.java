package com.planbvalidator.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.planbvalidator.config.PlanBProperties;
import com.planbvalidator.llm.PromptLoader;
import com.planbvalidator.pipeline.AnalysisPipelineMemory;
import com.planbvalidator.scoring.SalaryRangeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Optional;

/**
 * Gemini with Google Search grounding for corporate comp and Plan B market research.
 */
@Service
public class GeminiGroundedResearchService implements ResearchService {

    private static final Logger log = LoggerFactory.getLogger(GeminiGroundedResearchService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ResearchQueryBuilder queryBuilder;
    private final PlanBProperties properties;
    private final SalaryRangeParser salaryRangeParser;
    private final String systemPrompt;

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    public GeminiGroundedResearchService(ObjectMapper objectMapper,
                                         ResearchQueryBuilder queryBuilder,
                                         PlanBProperties properties,
                                         SalaryRangeParser salaryRangeParser,
                                         PromptLoader promptLoader) {
        this.objectMapper = objectMapper;
        this.queryBuilder = queryBuilder;
        this.properties = properties;
        this.salaryRangeParser = salaryRangeParser;
        this.systemPrompt = promptLoader.load("prompts/gemini-research-system.txt");
        this.webClient = WebClient.builder().baseUrl(properties.gemini().baseUrl()).build();
    }

    @Override
    public boolean isConfigured() {
        return properties.research().enabled()
                && properties.gemini().enabled()
                && apiKey != null
                && !apiKey.isBlank();
    }

    @Override
    public Optional<ResearchContext> fetchMarketContext(AnalysisPipelineMemory memory) {
        if (!isConfigured()) {
            log.info("research event=skipped reason=not_configured");
            return Optional.empty();
        }

        try {
            log.info("research event=started model={}", researchModel());
            Optional<ResearchContext> result = fetchWithPayload(queryBuilder.buildUserPayload(memory));
            if (result.isPresent()) {
                log.info("research event=completed sentiment={}", result.get().marketSentiment());
            } else {
                log.warn("research event=empty_response");
            }
            return result;
        } catch (Exception e) {
            log.warn("research event=failed model={} error={}", researchModel(), e.toString(), e);
            return Optional.empty();
        }
    }

    private Optional<ResearchContext> fetchWithPayload(String userPayload) throws Exception {
        String prompt = systemPrompt + "\n\nCandidate context (JSON):\n\n" + userPayload;

        ObjectNode body = objectMapper.createObjectNode();
        body.putArray("contents")
                .addObject()
                .putArray("parts")
                .addObject()
                .put("text", prompt);
        body.putArray("tools").addObject().set("google_search", objectMapper.createObjectNode());
        // JSON response mode is incompatible with google_search grounding — parse JSON from text instead.
        body.putObject("generationConfig").put("temperature", 0.2);

        String model = researchModel();
        String response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(properties.research().timeoutMs()))
                .block();

        if (response == null || response.isBlank()) {
            return Optional.empty();
        }

        String content = objectMapper.readTree(response)
                .path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(ResearchResponseParser.parse(content, objectMapper, salaryRangeParser));
    }

    private String researchModel() {
        String configured = properties.research().model();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return properties.gemini().model();
    }
}
