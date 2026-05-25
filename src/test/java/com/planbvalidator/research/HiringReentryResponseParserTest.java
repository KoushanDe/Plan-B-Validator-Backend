package com.planbvalidator.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiringReentryResponseParserTest {

    @Test
    void shouldParseReentryByGap() throws Exception {
        String json = """
                {
                  "summary": "Backend hiring remains active in Bengaluru.",
                  "overall_reentry_score": 72,
                  "overall_band": "moderate",
                  "recommended_minimum_gap_months": 6,
                  "reentry_by_gap": [
                    {"gap_months": 3, "gap_label": "3_months", "difficulty_score": 78, "difficulty_band": "easy",
                     "typical_weeks_to_offer_min": 4, "typical_weeks_to_offer_max": 10, "notes": "Short gap"},
                    {"gap_months": 6, "gap_label": "6_months", "difficulty_score": 65, "difficulty_band": "moderate",
                     "typical_weeks_to_offer_min": 6, "typical_weeks_to_offer_max": 14, "notes": "Moderate gap"},
                    {"gap_months": 9, "gap_label": "9_months", "difficulty_score": 52, "difficulty_band": "moderate",
                     "typical_weeks_to_offer_min": 8, "typical_weeks_to_offer_max": 18, "notes": "Longer gap"},
                    {"gap_months": 12, "gap_label": "12_plus_months", "difficulty_score": 40, "difficulty_band": "difficult",
                     "typical_weeks_to_offer_min": 12, "typical_weeks_to_offer_max": 24, "notes": "12+ months"}
                  ],
                  "salary_sources": ["Naukri", "LinkedIn"],
                  "market_notes": "Based on recent postings."
                }
                """;

        CurrentMarketConditionForHiring result = HiringReentryResponseParser.parse(json, new ObjectMapper());

        assertTrue(result.available());
        assertEquals(72, result.overallReentryScore());
        assertEquals(4, result.reentryByGap().size());
        assertEquals("3_months", result.reentryByGap().getFirst().gapLabel());
    }
}
