package com.sap.cloud.lm.sl.cf.web.helpers;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;

import javax.inject.Named;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Named
public class RateLimiterProvider {

    private Map<String, AtomicRateLimiter> rateLimiters = new ConcurrentHashMap<>();

    public AtomicRateLimiter getRateLimiter(String ipAddress) {
        return rateLimiters.computeIfAbsent(ipAddress, k -> createRateLimiter());
    }

    private AtomicRateLimiter createRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                                                    .limitForPeriod(5000)
                                                    .limitRefreshPeriod(Duration.ofHours(1))
                                                    .timeoutDuration(Duration.ZERO)
                                                    .build();
        return new AtomicRateLimiter("MTA Rate Limiter", config);
    }
}
