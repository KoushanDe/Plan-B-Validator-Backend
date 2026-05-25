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
     * @return empty if allowed; otherwise which limit was exceeded
     */
    public Optional<LimitScope> tryConsume(String userKey) {
        Bucket userBucket = perUserBuckets.computeIfAbsent(userKey,
                ignored -> newBucket(perUserLimit, Duration.ofMinutes(perUserWindowMinutes)));
        if (!globalBucket.estimateAbilityToConsume(1).canBeConsumed()) {
            return Optional.of(LimitScope.GLOBAL);
        }
        if (!userBucket.estimateAbilityToConsume(1).canBeConsumed()) {
            return Optional.of(LimitScope.PER_USER);
        }
        globalBucket.tryConsume(1);
        userBucket.tryConsume(1);
        return Optional.empty();
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
