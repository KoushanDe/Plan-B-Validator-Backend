package com.planbvalidator.research;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Web-backed assessment of how easy it is to re-enter the corporate job market after quitting,
 * broken down by employment-gap length (3, 6, 9, 12+ months).
 */
public record CurrentMarketConditionForHiring(
        String summary,
        int overallReentryScore,
        String overallBand,
        int recommendedMinimumGapMonths,
        List<ReentryGapOutlook> reentryByGap,
        List<String> salarySources,
        String marketNotes
) {
    public boolean available() {
        return summary != null && !summary.isBlank() && reentryByGap != null && !reentryByGap.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("summary", summary == null ? "" : summary);
        m.put("overall_reentry_score", overallReentryScore);
        m.put("overall_band", overallBand == null ? "" : overallBand);
        m.put("recommended_minimum_gap_months", recommendedMinimumGapMonths);
        m.put("reentry_by_gap", reentryByGap == null ? List.of()
                : reentryByGap.stream().map(ReentryGapOutlook::toMap).collect(Collectors.toList()));
        m.put("salary_sources", salarySources == null ? List.of() : salarySources);
        m.put("market_notes", marketNotes == null ? "" : marketNotes);
        return m;
    }

    public static CurrentMarketConditionForHiring unavailable() {
        return new CurrentMarketConditionForHiring("", 0, "unknown", 0, List.of(), List.of(), "");
    }
}
