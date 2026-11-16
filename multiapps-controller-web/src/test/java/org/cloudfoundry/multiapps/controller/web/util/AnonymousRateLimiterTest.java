package org.cloudfoundry.multiapps.controller.web.util;

import java.util.concurrent.TimeUnit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.util.BucketStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnonymousRateLimiterTest {

    private static final String IP = "203.0.113.42";
    private static final int PER_IP_CAPACITY = 50;
    private static final int PER_IP_REFILL_PER_HOUR = 50;
    private static final long NANOS_TO_WAIT = TimeUnit.SECONDS.toNanos(42);

    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private BucketStore bucketStore;
    @Mock
    private Bucket ipBucket;

    private AnonymousRateLimiter anonymousRateLimiter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(applicationConfiguration.getAnonymousRateLimitPerIpCapacity()).thenReturn(PER_IP_CAPACITY);
        when(applicationConfiguration.getAnonymousRateLimitPerIpRefillPerHour()).thenReturn(PER_IP_REFILL_PER_HOUR);
        anonymousRateLimiter = new AnonymousRateLimiter(applicationConfiguration, bucketStore);
    }

    @Test
    void testGetCapacityReturnsConfiguredCapacity() {
        assertEquals(PER_IP_CAPACITY, anonymousRateLimiter.getCapacity());
    }

    @Test
    void testTryConsumeUsesIpNamespacedBucket() {
        stubBucketForKey(AnonymousRateLimitKeys.ipKey(IP), ipBucket);
        stubConsumption(ipBucket, true);

        anonymousRateLimiter.tryConsume(IP);

        verify(bucketStore).getBucket(eq(AnonymousRateLimitKeys.ipKey(IP)), any(BucketConfiguration.class));
        verify(ipBucket).tryConsumeAndReturnRemaining(1);
    }

    @Test
    void testTryConsumeReturnsConsumedProbeWhenAllowed() {
        stubBucketForKey(AnonymousRateLimitKeys.ipKey(IP), ipBucket);
        stubConsumption(ipBucket, true);

        ConsumptionProbe probe = anonymousRateLimiter.tryConsume(IP);

        assertTrue(probe.isConsumed());
    }

    @Test
    void testTryConsumeReturnsRejectedProbeWhenExhausted() {
        stubBucketForKey(AnonymousRateLimitKeys.ipKey(IP), ipBucket);
        stubConsumption(ipBucket, false);

        ConsumptionProbe probe = anonymousRateLimiter.tryConsume(IP);

        assertFalse(probe.isConsumed());
        assertEquals(NANOS_TO_WAIT, probe.getNanosToWaitForRefill());
    }

    private void stubBucketForKey(long key, Bucket bucket) {
        when(bucketStore.getBucket(eq(key), any(BucketConfiguration.class))).thenReturn(bucket);
    }

    private void stubConsumption(Bucket bucket, boolean consumed) {
        ConsumptionProbe probe = consumed ? ConsumptionProbe.consumed(1, NANOS_TO_WAIT)
            : ConsumptionProbe.rejected(0, NANOS_TO_WAIT, NANOS_TO_WAIT);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    }
}
