package org.cloudfoundry.multiapps.controller.web.interceptors;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.util.AnonymousRateLimiter;
import org.cloudfoundry.multiapps.controller.web.util.ClientIpResolver;
import org.springframework.http.HttpStatus;

/**
 * Throttles requests by source IP using a cluster-aware token bucket. Unlike authentication, this choke point also covers paths that bypass
 * the security filter chain (such as {@code /public/**}) and anonymous callers, so it throttles every request regardless of whether an
 * {@code Authorization} header is present.
 */
@Named
public class RateLimitingInterceptor implements CustomHandlerInterceptor {

    private final AnonymousRateLimiter anonymousRateLimiter;
    private final ClientIpResolver clientIpResolver;

    @Inject
    public RateLimitingInterceptor(AnonymousRateLimiter anonymousRateLimiter, ClientIpResolver clientIpResolver) {
        this.anonymousRateLimiter = anonymousRateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ipAddress = clientIpResolver.resolve(request);
        ConsumptionProbe probe = anonymousRateLimiter.tryConsume(ipAddress);

        response.setHeader(Constants.RATE_LIMIT, Integer.toString(anonymousRateLimiter.getCapacity()));
        response.setHeader(Constants.RATE_LIMIT_REMAINING, Long.toString(probe.getRemainingTokens()));

        if (!probe.isConsumed()) {
            response.setHeader(Constants.RATE_LIMIT_RESET, Long.toString(getUtcTimeForNextReset(probe.getNanosToWaitForRefill())));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
            return false;
        }
        return true;
    }

    private static long getUtcTimeForNextReset(long nanosToWaitForReset) {
        return OffsetDateTime.now(ZoneOffset.UTC)
                             .plusNanos(nanosToWaitForReset)
                             .toEpochSecond();
    }

}
