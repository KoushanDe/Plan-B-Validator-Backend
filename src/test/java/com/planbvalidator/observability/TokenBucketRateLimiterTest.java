package com.planbvalidator.observability;

import com.planbvalidator.config.PlanBProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBucketRateLimiterTest {

    @Test
    void enforcesPerUserAndGlobalLimits() {
        var props = new PlanBProperties(
                null, null, null,
                new PlanBProperties.RateLimit(2, 60, 3),
                null
        );
        var limiter = new TokenBucketRateLimiter(props);

        assertTrue(limiter.wouldExceed("user-a").isEmpty());
        limiter.recordSuccessfulAnalyze("user-a");
        assertTrue(limiter.wouldExceed("user-a").isEmpty());
        limiter.recordSuccessfulAnalyze("user-a");
        assertEquals(TokenBucketRateLimiter.LimitScope.PER_USER, limiter.wouldExceed("user-a").orElseThrow());

        assertTrue(limiter.wouldExceed("user-b").isEmpty());
    }

    @Test
    void failedAnalyzeDoesNotConsumeQuota() {
        var props = new PlanBProperties(
                null, null, null,
                new PlanBProperties.RateLimit(1, 5, 10),
                null
        );
        var limiter = new TokenBucketRateLimiter(props);

        assertTrue(limiter.wouldExceed("user-a").isEmpty());
        // Simulate 500/400 — no recordSuccessfulAnalyze
        assertTrue(limiter.wouldExceed("user-a").isEmpty());
        limiter.recordSuccessfulAnalyze("user-a");
        assertEquals(TokenBucketRateLimiter.LimitScope.PER_USER, limiter.wouldExceed("user-a").orElseThrow());
    }

    @Test
    void enforcesGlobalLimitAcrossUsers() {
        var props = new PlanBProperties(
                null, null, null,
                new PlanBProperties.RateLimit(10, 60, 2),
                null
        );
        var limiter = new TokenBucketRateLimiter(props);

        assertTrue(limiter.wouldExceed("user-a").isEmpty());
        limiter.recordSuccessfulAnalyze("user-a");
        assertTrue(limiter.wouldExceed("user-b").isEmpty());
        limiter.recordSuccessfulAnalyze("user-b");
        assertEquals(TokenBucketRateLimiter.LimitScope.GLOBAL, limiter.wouldExceed("user-c").orElseThrow());
    }

    @Test
    void appliesOnlyToAnalyzePost() {
        var analyze = new MockHttpServletRequest("POST", "/v1/analyze");
        var runway = new MockHttpServletRequest("POST", "/v1/runway/calculate");
        var health = new MockHttpServletRequest("GET", "/v1/health");

        assertTrue(RateLimitFilter.isAnalyzeRequest(analyze));
        assertFalse(RateLimitFilter.isAnalyzeRequest(runway));
        assertFalse(RateLimitFilter.isAnalyzeRequest(health));
    }

    @Test
    void resolvesUserKeyFromHeaderOrIp() {
        var req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "abc-123");
        assertEquals("user:abc-123", RateLimitFilter.resolveUserKey(req));

        req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.5");
        assertEquals("ip:10.0.0.5", RateLimitFilter.resolveUserKey(req));

        req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        assertEquals("ip:203.0.113.1", RateLimitFilter.resolveUserKey(req));
    }
}
