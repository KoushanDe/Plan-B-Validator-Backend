package com.planbvalidator.domain.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * How the user intends to pursue Plan B.
 * Side hustle: lower financial risk, higher ongoing stress (dual workload).
 * Full-time leap: higher financial risk, focused transition.
 */
public enum PlanBEngagementMode {
    @JsonProperty("side_hustle")
    SIDE_HUSTLE,
    @JsonProperty("full_time_leap")
    FULL_TIME_LEAP
}
