package com.planbvalidator.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = Optional.ofNullable(request.getHeader("X-Request-Id")).filter(s -> !s.isBlank()).orElse(UUID.randomUUID().toString());
        request.setAttribute("request_id", requestId);
        response.setHeader("X-Request-Id", requestId);
        MDC.put("request_id", requestId);
        long started = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            HttpRequestLogger.logCompleted(request, response.getStatus(), System.currentTimeMillis() - started);
            MDC.remove("request_id");
        }
    }
}
