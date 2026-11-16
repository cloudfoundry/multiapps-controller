package org.cloudfoundry.multiapps.controller.web.util;

import java.util.concurrent.TimeUnit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.util.BucketStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FailedAuthRateLimiterTest {

    private static final String IP = "203.0.113.42";
    private static final String USER = "john.doe";
    private static final int PER_IP_CAPACITY = 20;
    private static final int PER_IP_REFILL_PER_HOUR = 20;
    private static final int PER_USER_CAPACITY = 10;
    private static final int PER_USER_REFILL_PER_HOUR = 10;
    private static final long NANOS_TO_WAIT = TimeUnit.SECONDS.toNanos(42);

    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private BucketStore bucketStore;
    @Mock
    private Bucket ipBucket;
    @Mock
    private Bucket userBucket;

    private FailedAuthRateLimiter failedAuthRateLimiter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(applicationConfiguration.getFailedAuthRateLimitPerIpCapacity()).thenReturn(PER_IP_CAPACITY);
        when(applicationConfiguration.getFailedAuthRateLimitPerIpRefillPerHour()).thenReturn(PER_IP_REFILL_PER_HOUR);
        when(applicationConfiguration.getFailedAuthRateLimitPerUserCapacity()).thenReturn(PER_USER_CAPACITY);
        when(applicationConfiguration.getFailedAuthRateLimitPerUserRefillPerHour()).thenReturn(PER_USER_REFILL_PER_HOUR);
        failedAuthRateLimiter = new FailedAuthRateLimiter(applicationConfiguration, bucketStore);
    }

    private void enableRateLimiting() {
        when(applicationConfiguration.isFailedAuthRateLimitingEnabled()).thenReturn(true);
    }

    @Test
    void testCheckAllowedIsNoOpWhenFeatureFlagOff() {
        when(applicationConfiguration.isFailedAuthRateLimitingEnabled()).thenReturn(false);

        assertDoesNotThrow(() -> failedAuthRateLimiter.checkAllowed(IP, USER));

        verifyNoInteractions(bucketStore);
    }

    @Test
    void testRecordFailedAttemptIsNoOpWhenFeatureFlagOff() {
        when(applicationConfiguration.isFailedAuthRateLimitingEnabled()).thenReturn(false);

        assertDoesNotThrow(() -> failedAuthRateLimiter.recordFailedAttempt(IP, USER));

        verifyNoInteractions(bucketStore);
    }

    @Test
    void testCheckAllowedDoesNotConsumeTokens() {
        enableRateLimiting();
        stubBucketForKey(FailedAuthRateLimitKeys.ipKey(IP), ipBucket);
        stubBucketForKey(FailedAuthRateLimitKeys.userKey(USER), userBucket);
        stubEstimation(ipBucket, true);
        stubEstimation(userBucket, true);

        assertDoesNotThrow(() -> failedAuthRateLimiter.checkAllowed(IP, USER));

        verify(ipBucket).estimateAbilityToConsume(1);
        verify(userBucket).estimateAbilityToConsume(1);
        verify(ipBucket, org.mockito.Mockito.never()).tryConsumeAndReturnRemaining(org.mockito.ArgumentMatchers.anyLong());
        verify(userBucket, org.mockito.Mockito.never()).tryConsumeAndReturnRemaining(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void testCheckAllowedInspectsOnlyIpBucketWhenUsernameIsNull() {
        enableRateLimiting();
        stubBucketForKey(FailedAuthRateLimitKeys.ipKey(IP), ipBucket);
        stubEstimation(ipBucket, true);

        assertDoesNotThrow(() -> failedAuthRateLimiter.checkAllowed(IP, null));

        verify(bucketStore).getBucket(eq(FailedAuthRateLimitKeys.ipKey(IP)), any(BucketConfiguration.class));
    }

    @Test
    void testCheckAllowedThrowsWhenIpBucketExhausted() {
        enableRateLimiting();
        stubBucketForKey(FailedAuthRateLimitKeys.ipKey(IP), ipBucket);
        stubEstimation(ipBucket, false);

        OperationRateLimitExceededException exception = assertThrows(OperationRateLimitExceededException.class,
                                                                     () -> failedAuthRateLimiter.checkAllowed(IP, USER));
        assertEquals(TimeUnit.NANOSECONDS.toSeconds(NANOS_TO_WAIT), exception.getRetryAfterSeconds());
    }

    @Test
    void testCheckAllowedThrowsWhenUserBucketExhausted() {
        enableRateLimiting();
        stubBucketForKey(FailedAuthRateLimitKeys.ipKey(IP), ipBucket);
        stubBucketForKey(FailedAuthRateLimitKeys.userKey(USER), userBucket);
        stubEstimation(ipBucket, true);
        stubEstimation(userBucket, false);

        OperationRateLimitExceededException exception = assertThrows(OperationRateLimitExceededException.class,
                                                                     () -> failedAuthRateLimiter.checkAllowed(IP, USER));
        assertEquals(TimeUnit.NANOSECONDS.toSeconds(NANOS_TO_WAIT), exception.getRetryAfterSeconds());
    }

    @Test
    void testRecordFailedAttemptConsumesIpAndUserTokens() {
        enableRateLimiting();
        stubBucketForKey(FailedAuthRateLimitKeys.ipKey(IP), ipBucket);
        stubBucketForKey(FailedAuthRateLimitKeys.userKey(USER), userBucket);
        stubConsumption(ipBucket, true);
        stubConsumption(userBucket, true);

        assertDoesNotThrow(() -> failedAuthRateLimiter.recordFailedAttempt(IP, USER));

        verify(bucketStore).getBucket(eq(FailedAuthRateLimitKeys.ipKey(IP)), any(BucketConfiguration.class));
        verify(bucketStore).getBucket(eq(FailedAuthRateLimitKeys.userKey(USER)), any(BucketConfiguration.class));
        verify(ipBucket).tryConsumeAndReturnRemaining(1);
        verify(userBucket).tryConsumeAndReturnRemaining(1);
    }

    @Test
    void testRecordFailedAttemptConsumesOnlyIpTokenWhenUsernameIsNull() {
        enableRateLimiting();
        stubBucketForKey(FailedAuthRateLimitKeys.ipKey(IP), ipBucket);
        stubConsumption(ipBucket, true);

        assertDoesNotThrow(() -> failedAuthRateLimiter.recordFailedAttempt(IP, null));

        verify(bucketStore).getBucket(eq(FailedAuthRateLimitKeys.ipKey(IP)), any(BucketConfiguration.class));
        verifyNoInteractions(userBucket);
    }

    @Test
    void testRecordFailedAttemptThrowsWhenIpBucketExhausted() {
        enableRateLimiting();
        stubBucketForKey(FailedAuthRateLimitKeys.ipKey(IP), ipBucket);
        stubConsumption(ipBucket, false);

        OperationRateLimitExceededException exception = assertThrows(OperationRateLimitExceededException.class,
                                                                     () -> failedAuthRateLimiter.recordFailedAttempt(IP, USER));
        assertEquals(TimeUnit.NANOSECONDS.toSeconds(NANOS_TO_WAIT), exception.getRetryAfterSeconds());
    }

    @Test
    void testRecordFailedAttemptThrowsWhenUserBucketExhausted() {
        enableRateLimiting();
        stubBucketForKey(FailedAuthRateLimitKeys.ipKey(IP), ipBucket);
        stubBucketForKey(FailedAuthRateLimitKeys.userKey(USER), userBucket);
        stubConsumption(ipBucket, true);
        stubConsumption(userBucket, false);

        OperationRateLimitExceededException exception = assertThrows(OperationRateLimitExceededException.class,
                                                                     () -> failedAuthRateLimiter.recordFailedAttempt(IP, USER));
        assertEquals(TimeUnit.NANOSECONDS.toSeconds(NANOS_TO_WAIT), exception.getRetryAfterSeconds());
    }

    private void stubBucketForKey(long key, Bucket bucket) {
        when(bucketStore.getBucket(eq(key), any(BucketConfiguration.class))).thenReturn(bucket);
    }

    private void stubEstimation(Bucket bucket, boolean canBeConsumed) {
        EstimationProbe probe = canBeConsumed ? EstimationProbe.canBeConsumed(1)
            : EstimationProbe.canNotBeConsumed(0, NANOS_TO_WAIT);
        when(bucket.estimateAbilityToConsume(1)).thenReturn(probe);
    }

    private void stubConsumption(Bucket bucket, boolean consumed) {
        ConsumptionProbe probe = consumed ? ConsumptionProbe.consumed(1, NANOS_TO_WAIT)
            : ConsumptionProbe.rejected(0, NANOS_TO_WAIT, NANOS_TO_WAIT);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    }
}
