package com.planbvalidator.observability;

import com.planbvalidator.domain.common.ErrorCode;
import com.planbvalidator.domain.response.ErrorEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ANALYZE_PATH = "/v1/analyze";

    private final TokenBucketRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(TokenBucketRateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isAnalyzeRequest(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userKey = resolveUserKey(request);
        Optional<TokenBucketRateLimiter.LimitScope> exceeded = rateLimiter.tryConsume(userKey);

        if (exceeded.isPresent()) {
            String requestId = String.valueOf(request.getAttribute("request_id"));
            TokenBucketRateLimiter.LimitScope scope = exceeded.get();
            String message;
            int retryAfterSeconds;
            if (scope == TokenBucketRateLimiter.LimitScope.GLOBAL) {
                message = "Analyze rate limit exceeded (%d requests/minute across all users)"
                        .formatted(rateLimiter.globalLimit());
                retryAfterSeconds = 60;
            } else {
                message = "Analyze rate limit exceeded (%d request(s) per %d minutes per user)"
                        .formatted(rateLimiter.perUserLimit(), rateLimiter.perUserWindowMinutes());
                retryAfterSeconds = rateLimiter.perUserWindowMinutes() * 60;
            }

            log.warn("rate_limit event=exceeded requestId={} scope={} userKey={} path={}",
                    requestId, scope, userKey, request.getRequestURI());

            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.getWriter().write(objectMapper.writeValueAsString(
                    ErrorEnvelope.of(ErrorCode.RATE_LIMITED, message, requestId)
            ));
            return;
        }
        filterChain.doFilter(request, response);
    }

    static boolean isAnalyzeRequest(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return ANALYZE_PATH.equals(uri) || uri.endsWith(ANALYZE_PATH);
    }

    /**
     * Prefer explicit client user id; fall back to client IP (supports X-Forwarded-For behind proxy/ngrok).
     */
    static String resolveUserKey(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String clientIp = forwarded.split(",")[0].trim();
            if (!clientIp.isBlank()) {
                return "ip:" + clientIp;
            }
        }
        return "ip:" + request.getRemoteAddr();
    }
}
