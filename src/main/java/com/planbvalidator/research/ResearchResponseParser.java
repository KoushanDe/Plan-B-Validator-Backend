package com.planbvalidator.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planbvalidator.llm.JsonResponseParser;
import com.planbvalidator.scoring.SalaryRangeParser;

import java.util.List;

final class ResearchResponseParser {

    private ResearchResponseParser() {
    }

    static ResearchContext parse(String content, ObjectMapper objectMapper, SalaryRangeParser salaryRangeParser)
            throws Exception {
        String jsonPayload = extractJson(content);
        JsonNode structured = objectMapper.readTree(jsonPayload);

        String summary = truncate(structured.path("market_summary").asText(""), 500);
        String salary = truncate(structured.path("salary_notes").asText(""), 500);
        String corporateRange = normalizeCorporateSalaryRange(
                truncate(JsonResponseParser.text(structured, "corporate_salary_range", ""), 200),
                salaryRangeParser
        );
        String planBIncomeRange = normalizeCorporateSalaryRange(
                truncate(JsonResponseParser.text(structured, "plan_b_realistic_income_range", ""), 200),
                salaryRangeParser
        );
        Integer typicalMonths = parseTypicalMonths(structured.path("typical_months_to_meaningful_income"));
        String planBNotes = truncate(JsonResponseParser.text(structured, "plan_b_market_notes", ""), 500);
        MarketSentiment sentiment = parseSentiment(structured.path("market_sentiment").asText("neutral"));
        int webScore = structured.path("web_competitiveness_score").asInt(50);
        boolean disagreement = structured.path("salary_disagreement_with_resume_inference").asBoolean(false);

        List<String> risks = objectMapper.convertValue(
                structured.path("risk_factors"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );
        List<String> sources = JsonResponseParser.stringList(objectMapper, structured, "salary_sources");

        return new ResearchContext(
                summary,
                sentiment,
                salary,
                risks == null ? List.of() : risks,
                corporateRange,
                planBNotes,
                sources,
                clampScore(webScore),
                disagreement,
                planBIncomeRange,
                typicalMonths
        );
    }

    private static String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private static MarketSentiment parseSentiment(String value) {
        return switch (value.toLowerCase().replace(' ', '_')) {
            case "positive" -> MarketSentiment.POSITIVE;
            case "moderately_positive" -> MarketSentiment.MODERATELY_POSITIVE;
            case "moderately_negative" -> MarketSentiment.MODERATELY_NEGATIVE;
            case "negative" -> MarketSentiment.NEGATIVE;
            default -> MarketSentiment.NEUTRAL;
        };
    }

    private static int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String normalizeCorporateSalaryRange(String raw, SalaryRangeParser salaryRangeParser) {
        if (raw == null || raw.isBlank()) {
            return raw == null ? "" : raw;
        }
        return salaryRangeParser.normalizeToInrLpa(raw).orElse(raw);
    }

    private static Integer parseTypicalMonths(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        int value = node.asInt(0);
        return value > 0 ? value : null;
    }
}
