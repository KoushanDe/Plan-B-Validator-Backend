package com.planbvalidator.research;

import java.util.LinkedHashMap;
import java.util.Map;

public record ReentryGapOutlook(
        int gapMonths,
        String gapLabel,
        int difficultyScore,
        String difficultyBand,
        Integer typicalWeeksToOfferMin,
        Integer typicalWeeksToOfferMax,
        String notes
) {
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("gap_months", gapMonths);
        m.put("gap_label", gapLabel);
        m.put("difficulty_score", difficultyScore);
        m.put("difficulty_band", difficultyBand);
        if (typicalWeeksToOfferMin != null) {
            m.put("typical_weeks_to_offer_min", typicalWeeksToOfferMin);
        }
        if (typicalWeeksToOfferMax != null) {
            m.put("typical_weeks_to_offer_max", typicalWeeksToOfferMax);
        }
        m.put("notes", notes == null ? "" : notes);
        return m;
    }
}
