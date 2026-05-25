package com.planbvalidator.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planbvalidator.llm.JsonResponseParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class HiringReentryResponseParser {

    private HiringReentryResponseParser() {
    }

    static CurrentMarketConditionForHiring parse(String content, ObjectMapper objectMapper) throws Exception {
        String jsonPayload = extractJson(content);
        JsonNode root = objectMapper.readTree(jsonPayload);

        String summary = truncate(root.path("summary").asText(""), 600);
        int overallScore = clamp(root.path("overall_reentry_score").asInt(50));
        String overallBand = normalizeBand(JsonResponseParser.text(root, "overall_band", "moderate"));
        int recommendedGap = Math.max(0, root.path("recommended_minimum_gap_months").asInt(0));
        String marketNotes = truncate(JsonResponseParser.text(root, "market_notes", ""), 500);
        List<String> sources = JsonResponseParser.stringList(objectMapper, root, "salary_sources");

        List<ReentryGapOutlook> gaps = parseGaps(root.path("reentry_by_gap"));
        if (gaps.isEmpty()) {
            gaps = defaultGaps();
        }

        return new CurrentMarketConditionForHiring(
                summary,
                overallScore,
                overallBand,
                recommendedGap,
                gaps,
                sources == null ? List.of() : sources,
                marketNotes
        );
    }

    private static List<ReentryGapOutlook> parseGaps(JsonNode array) {
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<ReentryGapOutlook> gaps = new ArrayList<>();
        for (JsonNode node : array) {
            int gapMonths = node.path("gap_months").asInt(0);
            String label = JsonResponseParser.text(node, "gap_label", gapLabelFor(gapMonths));
            int score = clamp(node.path("difficulty_score").asInt(50));
            String band = normalizeBand(JsonResponseParser.text(node, "difficulty_band", "moderate"));
            Integer minWeeks = parseOptionalInt(node, "typical_weeks_to_offer_min");
            Integer maxWeeks = parseOptionalInt(node, "typical_weeks_to_offer_max");
            String notes = truncate(node.path("notes").asText(""), 400);
            if (gapMonths > 0) {
                gaps.add(new ReentryGapOutlook(gapMonths, label, score, band, minWeeks, maxWeeks, notes));
            }
        }
        return gaps;
    }

    private static List<ReentryGapOutlook> defaultGaps() {
        return List.of(
                gapDefault(3, "3_months"),
                gapDefault(6, "6_months"),
                gapDefault(9, "9_months"),
                gapDefault(12, "12_plus_months")
        );
    }

    private static ReentryGapOutlook gapDefault(int months, String label) {
        return new ReentryGapOutlook(months, label, 50, "moderate", 8, 16, "");
    }

    private static String gapLabelFor(int gapMonths) {
        return switch (gapMonths) {
            case 3 -> "3_months";
            case 6 -> "6_months";
            case 9 -> "9_months";
            case 12 -> "12_plus_months";
            default -> gapMonths + "_months";
        };
    }

    private static Integer parseOptionalInt(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        int v = value.asInt(0);
        return v > 0 ? v : null;
    }

    private static String normalizeBand(String band) {
        if (band == null || band.isBlank()) {
            return "moderate";
        }
        return band.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
