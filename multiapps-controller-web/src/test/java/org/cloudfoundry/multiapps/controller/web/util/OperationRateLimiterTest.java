package org.cloudfoundry.multiapps.controller.web.util;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.util.BucketStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OperationRateLimiterTest {

    private static final String SPACE_GUID = "3d4d3f9a-1a2b-4c5d-8e9f-0a1b2c3d4e5f";
    private static final String USER = "john.doe";
    private static final int PER_SPACE_CAPACITY = 300;
    private static final int PER_SPACE_REFILL_PER_HOUR = 800;
    private static final int PER_USER_CAPACITY = 150;
    private static final int PER_USER_REFILL_PER_HOUR = 300;
    private static final int MAX_ACTIVE_OPERATIONS_PER_SPACE = 500;
    private static final int MAX_ACTIVE_OPERATIONS_PER_USER = 200;
    private static final long NANOS_TO_WAIT = TimeUnit.SECONDS.toNanos(42);

    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private OperationService operationService;
    @Mock
    private BucketStore bucketStore;
    @Mock
    private Bucket spaceBucket;
    @Mock
    private Bucket userBucket;
    @InjectMocks
    private OperationRateLimiter operationRateLimiter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    private void enableRateLimiting() {
        when(applicationConfiguration.isOperationRateLimitingEnabled()).thenReturn(true);
    }

    private void stubRateLimitConfiguration() {
        when(applicationConfiguration.getMaxActiveOperationsPerSpace()).thenReturn(MAX_ACTIVE_OPERATIONS_PER_SPACE);
        when(applicationConfiguration.getMaxActiveOperationsPerUser()).thenReturn(MAX_ACTIVE_OPERATIONS_PER_USER);
        when(applicationConfiguration.getOperationRateLimitPerSpaceCapacity()).thenReturn(PER_SPACE_CAPACITY);
        when(applicationConfiguration.getOperationRateLimitPerSpaceRefillPerHour()).thenReturn(PER_SPACE_REFILL_PER_HOUR);
        when(applicationConfiguration.getOperationRateLimitPerUserCapacity()).thenReturn(PER_USER_CAPACITY);
        when(applicationConfiguration.getOperationRateLimitPerUserRefillPerHour()).thenReturn(PER_USER_REFILL_PER_HOUR);
    }

    @Test
    void testAllowedWhenFeatureFlagOffAndNothingIsTouched() {
        when(applicationConfiguration.isOperationRateLimitingEnabled()).thenReturn(false);

        assertDoesNotThrow(() -> operationRateLimiter.checkStartAllowed(USER, SPACE_GUID));

        verifyNoInteractions(operationService);
        verifyNoInteractions(bucketStore);
    }

    @Test
    void testCheckStartAllowedWhenUnderAllLimits() {
        enableRateLimiting();
        stubRateLimitConfiguration();
        stubActiveOperationCounts(0, 0);
        stubBucketForKey(OperationRateLimitKeys.spaceKey(SPACE_GUID), spaceBucket);
        stubBucketForKey(OperationRateLimitKeys.userKey(SPACE_GUID, USER), userBucket);
        stubConsumption(spaceBucket, true);
        stubConsumption(userBucket, true);

        assertDoesNotThrow(() -> operationRateLimiter.checkStartAllowed(USER, SPACE_GUID));
    }

    @Test
    void testChecksSpaceAndUserBucketsIndependently() {
        enableRateLimiting();
        stubRateLimitConfiguration();
        stubActiveOperationCounts(0, 0);
        stubBucketForKey(OperationRateLimitKeys.spaceKey(SPACE_GUID), spaceBucket);
        stubBucketForKey(OperationRateLimitKeys.userKey(SPACE_GUID, USER), userBucket);
        stubConsumption(spaceBucket, true);
        stubConsumption(userBucket, true);

        operationRateLimiter.checkStartAllowed(USER, SPACE_GUID);

        verify(bucketStore).getBucket(eq(OperationRateLimitKeys.spaceKey(SPACE_GUID)), any());
        verify(bucketStore).getBucket(eq(OperationRateLimitKeys.userKey(SPACE_GUID, USER)), any());
    }

    @Test
    void testThrowExceptionWhenSpaceBucketExhausted() {
        enableRateLimiting();
        stubRateLimitConfiguration();
        stubActiveOperationCounts(0, 0);
        stubBucketForKey(OperationRateLimitKeys.spaceKey(SPACE_GUID), spaceBucket);
        stubConsumption(spaceBucket, false);

        OperationRateLimitExceededException exception = assertThrows(OperationRateLimitExceededException.class,
                                                                     () -> operationRateLimiter.checkStartAllowed(USER, SPACE_GUID));
        assertEquals(TimeUnit.NANOSECONDS.toSeconds(NANOS_TO_WAIT), exception.getRetryAfterSeconds());
    }

    @Test
    void testThrowExceptionWhenUserBucketExhausted() {
        enableRateLimiting();
        stubRateLimitConfiguration();
        stubActiveOperationCounts(0, 0);
        stubBucketForKey(OperationRateLimitKeys.spaceKey(SPACE_GUID), spaceBucket);
        stubBucketForKey(OperationRateLimitKeys.userKey(SPACE_GUID, USER), userBucket);
        stubConsumption(spaceBucket, true);
        stubConsumption(userBucket, false);

        OperationRateLimitExceededException exception = assertThrows(OperationRateLimitExceededException.class,
                                                                     () -> operationRateLimiter.checkStartAllowed(USER, SPACE_GUID));
        assertEquals(TimeUnit.NANOSECONDS.toSeconds(NANOS_TO_WAIT), exception.getRetryAfterSeconds());
    }

    @Test
    void testThrowExceptionWhenActiveOperationsPerSpaceReachCap() {
        enableRateLimiting();
        when(applicationConfiguration.getMaxActiveOperationsPerSpace()).thenReturn(MAX_ACTIVE_OPERATIONS_PER_SPACE);
        stubActiveOperationCounts(MAX_ACTIVE_OPERATIONS_PER_SPACE, 0);

        assertThrows(OperationRateLimitExceededException.class, () -> operationRateLimiter.checkStartAllowed(USER, SPACE_GUID));
        verifyNoInteractions(bucketStore);
    }

    @Test
    void testThrowExceptionWhenActiveOperationsPerUserReachCap() {
        enableRateLimiting();
        when(applicationConfiguration.getMaxActiveOperationsPerSpace()).thenReturn(MAX_ACTIVE_OPERATIONS_PER_SPACE);
        when(applicationConfiguration.getMaxActiveOperationsPerUser()).thenReturn(MAX_ACTIVE_OPERATIONS_PER_USER);
        stubActiveOperationCounts(0, MAX_ACTIVE_OPERATIONS_PER_USER);

        assertThrows(OperationRateLimitExceededException.class, () -> operationRateLimiter.checkStartAllowed(USER, SPACE_GUID));
        verifyNoInteractions(bucketStore);
    }

    private void stubActiveOperationCounts(int perSpace, int perUser) {
        OperationQuery spaceQuery = mockQuery();
        when(spaceQuery.spaceId(SPACE_GUID)).thenReturn(spaceQuery);
        when(spaceQuery.inNonFinalState()).thenReturn(spaceQuery);
        when(spaceQuery.list()).thenReturn(activeOperations(perSpace));

        OperationQuery userQuery = mockQuery();
        when(userQuery.user(USER)).thenReturn(userQuery);
        when(userQuery.spaceId(SPACE_GUID)).thenReturn(userQuery);
        when(userQuery.inNonFinalState()).thenReturn(userQuery);
        when(userQuery.list()).thenReturn(activeOperations(perUser));

        when(operationService.createQuery()).thenReturn(spaceQuery, userQuery);
    }

    private OperationQuery mockQuery() {
        return mock(OperationQuery.class);
    }

    private List<Operation> activeOperations(int count) {
        if (count == 0) {
            return Collections.emptyList();
        }
        return Stream.generate(() -> mock(Operation.class))
                     .limit(count)
                     .toList();
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
