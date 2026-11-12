package org.cloudfoundry.multiapps.controller.web.interceptors;

import java.util.concurrent.TimeUnit;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.util.AnonymousRateLimiter;
import org.cloudfoundry.multiapps.controller.web.util.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitingInterceptorTest {

    private static final String CLIENT_IP = "203.0.113.42";
    private static final int CAPACITY = 5000;
    private static final long REMAINING = 4999;
    private static final long NANOS_TO_WAIT = TimeUnit.SECONDS.toNanos(42);

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private AnonymousRateLimiter anonymousRateLimiter;
    @Mock
    private ClientIpResolver clientIpResolver;

    private RateLimitingInterceptor interceptor;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(anonymousRateLimiter.getCapacity())
               .thenReturn(CAPACITY);
        Mockito.when(clientIpResolver.resolve(request))
               .thenReturn(CLIENT_IP);
        interceptor = new RateLimitingInterceptor(anonymousRateLimiter, clientIpResolver);
    }

    @Test
    void throttlesByResolvedClientIpAndSetsHeadersWhenAllowed() throws Exception {
        Mockito.when(anonymousRateLimiter.tryConsume(CLIENT_IP))
               .thenReturn(ConsumptionProbe.consumed(REMAINING, NANOS_TO_WAIT));

        assertTrue(interceptor.preHandle(request, response, new Object()));

        Mockito.verify(clientIpResolver)
               .resolve(request);
        Mockito.verify(anonymousRateLimiter)
               .tryConsume(CLIENT_IP);
        Mockito.verify(response)
               .setHeader(Constants.RATE_LIMIT, Integer.toString(CAPACITY));
        Mockito.verify(response)
               .setHeader(Constants.RATE_LIMIT_REMAINING, Long.toString(REMAINING));
        Mockito.verify(response, Mockito.never())
               .sendError(Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    void doesNotSkipWhenAuthorizationHeaderPresent() throws Exception {
        Mockito.when(anonymousRateLimiter.tryConsume(CLIENT_IP))
               .thenReturn(ConsumptionProbe.consumed(REMAINING, NANOS_TO_WAIT));

        assertTrue(interceptor.preHandle(request, response, new Object()));

        Mockito.verify(anonymousRateLimiter)
               .tryConsume(CLIENT_IP);
        Mockito.verify(request, Mockito.never())
               .getHeader(org.springframework.http.HttpHeaders.AUTHORIZATION);
    }

    @Test
    void rejectsRequestWith429WhenLimitExceeded() throws Exception {
        Mockito.when(anonymousRateLimiter.tryConsume(CLIENT_IP))
               .thenReturn(ConsumptionProbe.rejected(0, NANOS_TO_WAIT, NANOS_TO_WAIT));

        assertFalse(interceptor.preHandle(request, response, new Object()));

        Mockito.verify(response)
               .setHeader(Constants.RATE_LIMIT, Integer.toString(CAPACITY));
        Mockito.verify(response)
               .setHeader(Constants.RATE_LIMIT_REMAINING, Long.toString(0));
        Mockito.verify(response)
               .setHeader(Mockito.eq(Constants.RATE_LIMIT_RESET), Mockito.anyString());
        Mockito.verify(response)
               .sendError(HttpStatus.TOO_MANY_REQUESTS.value(), HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
    }

}
