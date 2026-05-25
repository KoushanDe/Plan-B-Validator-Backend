package com.planbvalidator.api;

import com.planbvalidator.domain.common.ErrorCode;
import com.planbvalidator.domain.response.ErrorEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(this::toMessage)
                .orElse("Invalid input");
        log.warn("api event=validation_failed requestId={} message={}", requestId(request), message);
        return ResponseEntity.badRequest().body(ErrorEnvelope.of(ErrorCode.INVALID_INPUT, message, requestId(request)));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorEnvelope> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex,
                                                                    HttpServletRequest request) {
        String message = "Unsupported Content-Type. For multipart analyze, send part 'request' as JSON "
                + "(application/json or application/octet-stream with JSON body). "
                + "For JSON-only analyze, use Content-Type: application/json.";
        log.warn("api event=unsupported_media_type requestId={} detail={}", requestId(request), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorEnvelope.of(ErrorCode.INVALID_INPUT, message, requestId(request)));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorEnvelope> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                             HttpServletRequest request) {
        log.warn("api event=not_acceptable requestId={} detail={}", requestId(request), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorEnvelope.of(ErrorCode.INVALID_INPUT, ex.getMessage(), requestId(request)));
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorEnvelope> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("api event=bad_request requestId={} message={}", requestId(request), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorEnvelope.of(ErrorCode.INVALID_INPUT, ex.getMessage(), requestId(request)));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorEnvelope> handleMissingPart(MissingServletRequestPartException ex,
                                                           HttpServletRequest request) {
        String message = "Missing multipart part '" + ex.getRequestPartName() + "'. "
                + "Send AnalyzeRequest JSON as part 'request' (Blob or string), optional PDF as 'resume'. "
                + "Without a resume, POST application/json instead.";
        log.warn("api event=missing_multipart_part requestId={} part={}", requestId(request), ex.getRequestPartName());
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorEnvelope.of(ErrorCode.INVALID_INPUT, message, requestId(request)));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorEnvelope> handleStatus(ResponseStatusException ex, HttpServletRequest request) {
        ErrorCode code = ex.getStatusCode() == HttpStatus.BAD_REQUEST ? ErrorCode.INVALID_INPUT : ErrorCode.INTERNAL_ERROR;
        if (ex.getStatusCode().is4xxClientError()) {
            log.warn("api event=client_error requestId={} status={} message={}",
                    requestId(request), ex.getStatusCode().value(), ex.getReason());
        } else {
            log.error("api event=client_error requestId={} status={} message={}",
                    requestId(request), ex.getStatusCode().value(), ex.getReason(), ex);
        }
        return ResponseEntity.status(ex.getStatusCode())
                .body(ErrorEnvelope.of(code, ex.getReason(), requestId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("api event=internal_error requestId={}", requestId(request), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorEnvelope.of(ErrorCode.INTERNAL_ERROR, "Unexpected internal error", requestId(request)));
    }

    private String toMessage(FieldError fieldError) {
        return "%s %s".formatted(fieldError.getField(), Optional.ofNullable(fieldError.getDefaultMessage()).orElse("is invalid"));
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute("request_id");
        return requestId == null ? "unknown" : requestId.toString();
    }
}
