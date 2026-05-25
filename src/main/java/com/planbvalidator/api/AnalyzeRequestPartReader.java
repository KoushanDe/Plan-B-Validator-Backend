package com.planbvalidator.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planbvalidator.domain.request.AnalyzeRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;

/**
 * Reads the multipart {@code request} part as JSON regardless of part Content-Type.
 * Browsers and AI Studio often send {@code application/octet-stream} for Blob parts.
 */
@Component
public class AnalyzeRequestPartReader {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AnalyzeRequestPartReader(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public AnalyzeRequest readJson(String json) {
        if (json == null || json.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing analyze JSON body");
        }
        try {
            AnalyzeRequest request = objectMapper.readValue(json, AnalyzeRequest.class);
            validate(request);
            return request;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON in analyze request");
        }
    }

    public AnalyzeRequest readPart(MultipartFile requestPart) {
        if (requestPart == null || requestPart.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing multipart part 'request'");
        }
        try {
            AnalyzeRequest request = objectMapper.readValue(requestPart.getInputStream(), AnalyzeRequest.class);
            validate(request);
            return request;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON in multipart part 'request'");
        }
    }

    private void validate(AnalyzeRequest request) {
        Set<ConstraintViolation<AnalyzeRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
