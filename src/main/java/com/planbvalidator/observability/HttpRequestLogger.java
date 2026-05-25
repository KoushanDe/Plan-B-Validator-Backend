package com.planbvalidator.observability;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight HTTP access logs for API debugging (method, path, status, duration).
 */
public final class HttpRequestLogger {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestLogger.class);

    private HttpRequestLogger() {
    }

    public static void logCompleted(HttpServletRequest request, int status, long durationMs) {
        String requestId = String.valueOf(request.getAttribute("request_id"));
        log.info("http event=request_completed requestId={} method={} path={} status={} durationMs={}",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                status,
                durationMs);
    }
}
