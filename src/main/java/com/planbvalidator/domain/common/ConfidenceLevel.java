package com.planbvalidator.domain.common;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ConfidenceLevel {
    @JsonProperty("low")
    LOW,
    @JsonProperty("medium")
    MEDIUM,
    @JsonProperty("high")
    HIGH
}
