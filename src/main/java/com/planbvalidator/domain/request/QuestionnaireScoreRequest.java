package com.planbvalidator.domain.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record QuestionnaireScoreRequest(@NotNull @Valid PsychologyDto psychology) {
}
