package com.planbvalidator.psychology;

public record PsychologyQuestion(
        String id,
        String field,
        String question,
        boolean invert
) {
}
