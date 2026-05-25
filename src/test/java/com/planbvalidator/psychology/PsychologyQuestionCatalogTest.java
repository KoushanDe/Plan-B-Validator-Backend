package com.planbvalidator.psychology;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PsychologyQuestionCatalogTest {

    @Test
    void shouldLoadTenQuestionsWithScale() throws Exception {
        var catalog = new PsychologyQuestionCatalog(new ObjectMapper());
        assertEquals(10, catalog.questions().size());
        assertEquals(5, catalog.scaleLabels().size());
        assertFalse(catalog.questions().getFirst().question().isBlank());
    }
}
