package com.planbvalidator.domain.response;

import com.planbvalidator.domain.common.ErrorCode;

public record ErrorEnvelope(ErrorBody error) {
    public static ErrorEnvelope of(ErrorCode code, String message, String requestId) {
        return new ErrorEnvelope(new ErrorBody(code.name(), message, requestId));
    }

    public record ErrorBody(String code, String message, String requestId) {
    }
}
