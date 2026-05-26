package com.planbvalidator.observability;

import com.planbvalidator.config.PlanBProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-bucket limits for analyze: per-user window + global per minute (both must allow).
 */
@Component
public class TokenBucketRateLimiter {

    public enum LimitScope {
        GLOBAL,
        PER_USER
    }

    private final Bucket globalBucket;
    private final Map<String, Bucket> perUserBuckets = new ConcurrentHashMap<>();
    private final int perUserLimit;
    private final int perUserWindowMinutes;
    private final int globalLimit;

    public TokenBucketRateLimiter(PlanBProperties properties) {
        var limits = properties.rateLimit();
        this.perUserLimit = limits.analyzePerUserRequests();
        this.perUserWindowMinutes = limits.analyzePerUserWindowMinutes();
        this.globalLimit = limits.analyzeGlobalRequestsPerMinute();
        this.globalBucket = newBucket(globalLimit, Duration.ofMinutes(1));
    }

    /**
     * Checks capacity without consuming (call before handling analyze).
     *
     * @return empty if a successful analyze could be recorded; otherwise which limit would be exceeded
     */
    public Optional<LimitScope> wouldExceed(String userKey) {
        Bucket userBucket = userBucket(userKey);
        if (!globalBucket.estimateAbilityToConsume(1).canBeConsumed()) {
            return Optional.of(LimitScope.GLOBAL);
        }
        if (!userBucket.estimateAbilityToConsume(1).canBeConsumed()) {
            return Optional.of(LimitScope.PER_USER);
        }
        return Optional.empty();
    }

    /**
     * Consumes one analyze slot on both buckets. Call only after {@code POST /v1/analyze} returns HTTP 200.
     */
    public void recordSuccessfulAnalyze(String userKey) {
        Bucket userBucket = userBucket(userKey);
        globalBucket.tryConsume(1);
        userBucket.tryConsume(1);
    }

    private Bucket userBucket(String userKey) {
        return perUserBuckets.computeIfAbsent(userKey,
                ignored -> newBucket(perUserLimit, Duration.ofMinutes(perUserWindowMinutes)));
    }

    int perUserLimit() {
        return perUserLimit;
    }

    int perUserWindowMinutes() {
        return perUserWindowMinutes;
    }

    int globalLimit() {
        return globalLimit;
    }

    private static Bucket newBucket(int capacity, Duration period) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, period)))
                .build();
    }
}
