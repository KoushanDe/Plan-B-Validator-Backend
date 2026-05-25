package com.planbvalidator.domain.common;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Verdict {
    @JsonProperty("take_the_leap")
    TAKE_THE_LEAP,
    @JsonProperty("take_with_caution")
    TAKE_WITH_CAUTION,
    @JsonProperty("delay")
    DELAY,
    @JsonProperty("do_not_take_now")
    DO_NOT_TAKE_NOW
}
