package org.cloudfoundry.multiapps.controller.web.interceptors;

import java.time.Duration;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.cloudfoundry.multiapps.controller.web.util.RateLimiterProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitingInterceptorTest {

    private static final String X_CF_TRUE_CLIENT_IP = "X-CF-True-Client-IP";
    private static final String CLIENT_IP = "203.0.113.42";
    private static final String REMOTE_ADDR = "10.0.0.1";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private RateLimiterProvider rateLimiterProvider;

    private RateLimitingInterceptor interceptor;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        interceptor = new RateLimitingInterceptor(rateLimiterProvider);
    }

    @Test
    void usesTrueClientIpHeaderWhenPresent() throws Exception {
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION))
               .thenReturn(null);
        Mockito.when(request.getHeader(X_CF_TRUE_CLIENT_IP))
               .thenReturn(CLIENT_IP);
        AtomicRateLimiter rateLimiter = newRateLimiter();
        Mockito.when(rateLimiterProvider.getRateLimiter(CLIENT_IP))
               .thenReturn(rateLimiter);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        Mockito.verify(rateLimiterProvider)
               .getRateLimiter(CLIENT_IP);
        Mockito.verify(request, Mockito.never())
               .getRemoteAddr();
    }

    @Test
    void fallsBackToRemoteAddrWhenHeaderAbsent() throws Exception {
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION))
               .thenReturn(null);
        Mockito.when(request.getHeader(X_CF_TRUE_CLIENT_IP))
               .thenReturn(null);
        Mockito.when(request.getRemoteAddr())
               .thenReturn(REMOTE_ADDR);
        AtomicRateLimiter rateLimiter = newRateLimiter();
        Mockito.when(rateLimiterProvider.getRateLimiter(REMOTE_ADDR))
               .thenReturn(rateLimiter);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        Mockito.verify(rateLimiterProvider)
               .getRateLimiter(REMOTE_ADDR);
    }

    @Test
    void fallsBackToRemoteAddrWhenHeaderBlank() throws Exception {
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION))
               .thenReturn(null);
        Mockito.when(request.getHeader(X_CF_TRUE_CLIENT_IP))
               .thenReturn("   ");
        Mockito.when(request.getRemoteAddr())
               .thenReturn(REMOTE_ADDR);
        AtomicRateLimiter rateLimiter = newRateLimiter();
        Mockito.when(rateLimiterProvider.getRateLimiter(REMOTE_ADDR))
               .thenReturn(rateLimiter);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        Mockito.verify(rateLimiterProvider)
               .getRateLimiter(REMOTE_ADDR);
    }

    @Test
    void skipsRateLimitingWhenAuthorizationHeaderPresent() throws Exception {
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION))
               .thenReturn("Bearer token");

        assertTrue(interceptor.preHandle(request, response, new Object()));

        Mockito.verifyNoInteractions(rateLimiterProvider);
    }

    @Test
    void rejectsRequestWith429WhenLimitExceeded() throws Exception {
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION))
               .thenReturn(null);
        Mockito.when(request.getHeader(X_CF_TRUE_CLIENT_IP))
               .thenReturn(CLIENT_IP);
        AtomicRateLimiter rateLimiter = exhaustedRateLimiter();
        Mockito.when(rateLimiterProvider.getRateLimiter(CLIENT_IP))
               .thenReturn(rateLimiter);

        assertFalse(interceptor.preHandle(request, response, new Object()));

        Mockito.verify(response)
               .sendError(HttpStatus.TOO_MANY_REQUESTS.value(), HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
    }

    private static AtomicRateLimiter newRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                                                    .limitForPeriod(10)
                                                    .limitRefreshPeriod(Duration.ofHours(1))
                                                    .timeoutDuration(Duration.ZERO)
                                                    .build();
        return new AtomicRateLimiter("test", config);
    }

    private static AtomicRateLimiter exhaustedRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                                                    .limitForPeriod(1)
                                                    .limitRefreshPeriod(Duration.ofHours(1))
                                                    .timeoutDuration(Duration.ZERO)
                                                    .build();
        AtomicRateLimiter rateLimiter = new AtomicRateLimiter("test", config);
        rateLimiter.acquirePermission();
        return rateLimiter;
    }

}
