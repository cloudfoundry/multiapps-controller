package com.sap.cloud.lm.sl.cf.web.security;

import com.sap.cloud.lm.sl.cf.web.Constants;
import com.sap.cloud.lm.sl.cf.web.helpers.RateLimiterProvider;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter.AtomicRateLimiterMetrics;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Named("rateLimiter")
public class RateLimiterFilter extends OncePerRequestFilter {

    @Inject
    private RateLimiterProvider rateLimiterProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
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
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static long getUtcTimeForNextReset(long nanosToWaitForReset) {
        return OffsetDateTime.now(ZoneOffset.UTC)
                             .plusNanos(nanosToWaitForReset)
                             .toEpochSecond();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.AUTHORIZATION) != null;
    }
}
