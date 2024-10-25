package org.cloudfoundry.multiapps.controller.web.interceptors;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.inject.Inject;
import javax.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.util.RateLimiterProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter.AtomicRateLimiterMetrics;

@Named
public class RateLimitingInterceptor implements CustomHandlerInterceptor {

    private final RateLimiterProvider rateLimiterProvider;

    @Inject
    public RateLimitingInterceptor(RateLimiterProvider rateLimiterProvider) {
        this.rateLimiterProvider = rateLimiterProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!shouldRateLimit(request)) {
            return true;
        }
        String ipAddress = request.getRemoteAddr();
        AtomicRateLimiter rateLimiter = rateLimiterProvider.getRateLimiter(ipAddress);

        boolean hasAcquiredPermission = rateLimiter.acquirePermission();
        AtomicRateLimiterMetrics metrics = rateLimiter.getDetailedMetrics();
        RateLimiterConfig config = rateLimiter.getRateLimiterConfig();

        response.setHeader(Constants.RATE_LIMIT, Integer.toString(config.getLimitForPeriod()));
        response.setHeader(Constants.RATE_LIMIT_REMAINING, Integer.toString(metrics.getAvailablePermissions()));

        if (!hasAcquiredPermission) {
            response.setHeader(Constants.RATE_LIMIT_RESET, Long.toString(getUtcTimeForNextReset(metrics.getNanosToWait())));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
            return false;
        }
        return true;
    }

    private static boolean shouldRateLimit(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.AUTHORIZATION) == null;
    }

    private static long getUtcTimeForNextReset(long nanosToWaitForReset) {
        return OffsetDateTime.now(ZoneOffset.UTC)
                             .plusNanos(nanosToWaitForReset)
                             .toEpochSecond();
    }

}
