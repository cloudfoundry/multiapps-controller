package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.util.BucketStore;

class OperationRateLimitBucketCleanerTest {

    private static final int SELECTED_INSTANCE = 0;
    private static final int OTHER_INSTANCE = 3;
    private static final int BATCH_SIZE = 1000;

    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private BucketStore bucketStore;
    @InjectMocks
    private OperationRateLimitBucketCleaner cleaner;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testDoesNothingWhenRateLimitingDisabled() {
        when(applicationConfiguration.isOperationRateLimitingEnabled()).thenReturn(false);

        cleaner.cleanUpExpiredBuckets();

        verifyNoInteractions(bucketStore);
    }

    @Test
    void testDoesNothingWhenNotSelectedInstance() {
        when(applicationConfiguration.isOperationRateLimitingEnabled()).thenReturn(true);
        when(applicationConfiguration.getApplicationInstanceIndex()).thenReturn(OTHER_INSTANCE);

        cleaner.cleanUpExpiredBuckets();

        verifyNoInteractions(bucketStore);
    }

    @Test
    void testDeletesInASingleBatchWhenFewerThanBatchSizeExpired() {
        enableCleaningOnSelectedInstance();
        when(bucketStore.removeExpiredEntries(BATCH_SIZE)).thenReturn(0);

        cleaner.cleanUpExpiredBuckets();

        verify(bucketStore, times(1)).removeExpiredEntries(BATCH_SIZE);
    }

    @Test
    void testKeepsDeletingUntilBatchNotFull() {
        enableCleaningOnSelectedInstance();
        when(bucketStore.removeExpiredEntries(BATCH_SIZE)).thenReturn(BATCH_SIZE, 30);

        cleaner.cleanUpExpiredBuckets();

        verify(bucketStore, times(2)).removeExpiredEntries(BATCH_SIZE);
    }

    @Test
    void testSwallowsExceptionFromBucketStore() {
        enableCleaningOnSelectedInstance();
        when(bucketStore.removeExpiredEntries(BATCH_SIZE)).thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(() -> cleaner.cleanUpExpiredBuckets());
    }

    private void enableCleaningOnSelectedInstance() {
        when(applicationConfiguration.isOperationRateLimitingEnabled()).thenReturn(true);
        when(applicationConfiguration.getApplicationInstanceIndex()).thenReturn(SELECTED_INSTANCE);
    }
}
