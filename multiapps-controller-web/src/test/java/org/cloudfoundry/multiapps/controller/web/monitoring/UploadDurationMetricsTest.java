package org.cloudfoundry.multiapps.controller.web.monitoring;

import org.cloudfoundry.multiapps.controller.persistence.monitoring.UploadDurationTracker;
import org.cloudfoundry.multiapps.controller.persistence.monitoring.UploadDurationTracker.UploadPathStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadDurationMetricsTest {

    @Mock
    private UploadDurationTracker uploadDurationTracker;

    @Mock
    private UploadPathStatistics appBinaryStatistics;

    @Mock
    private UploadPathStatistics objectStoreStatistics;

    @Mock
    private AppUploaderThreadPoolInformation appUploaderInfo;

    private UploadDurationMetrics uploadDurationMetrics;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(uploadDurationTracker.getAppBinaryStatistics())
            .thenReturn(appBinaryStatistics);
        when(uploadDurationTracker.getObjectStoreStatistics())
            .thenReturn(objectStoreStatistics);
        uploadDurationMetrics = new UploadDurationMetrics(uploadDurationTracker, appUploaderInfo);
    }

    @Test
    void testGetAppBinaryUploadTotalCount() {
        when(appBinaryStatistics.totalCount())
            .thenReturn(11L);
        assertEquals(11L, uploadDurationMetrics.getAppBinaryUploadTotalCount());
        verify(appBinaryStatistics)
            .totalCount();
    }

    @Test
    void testGetAppBinaryUploadTimeoutCount() {
        when(appBinaryStatistics.timeoutCount())
            .thenReturn(12L);
        assertEquals(12L, uploadDurationMetrics.getAppBinaryUploadTimeoutCount());
        verify(appBinaryStatistics)
            .timeoutCount();
    }

    @Test
    void testGetAppBinaryUploadMaxDurationMs() {
        when(appBinaryStatistics.maxDurationMs())
            .thenReturn(13L);
        assertEquals(13L, uploadDurationMetrics.getAppBinaryUploadMaxDurationMs());
        verify(appBinaryStatistics)
            .maxDurationMs();
    }

    @Test
    void testGetAppBinaryUploadSumDurationMs() {
        when(appBinaryStatistics.sumDurationMs())
            .thenReturn(14L);
        assertEquals(14L, uploadDurationMetrics.getAppBinaryUploadSumDurationMs());
        verify(appBinaryStatistics)
            .sumDurationMs();
    }

    @Test
    void testGetAppUploaderActiveThreads() {
        when(appUploaderInfo.getActiveThreads())
            .thenReturn(3);
        assertEquals(3, uploadDurationMetrics.getAppUploaderActiveThreads());
        verify(appUploaderInfo)
            .getActiveThreads();
    }

    @Test
    void testGetAppUploaderMaxThreads() {
        when(appUploaderInfo.getMaxThreads())
            .thenReturn(7);
        assertEquals(7, uploadDurationMetrics.getAppUploaderMaxThreads());
        verify(appUploaderInfo)
            .getMaxThreads();
    }

    @Test
    void testGetAppUploaderRejectionCount() {
        when(appBinaryStatistics.rejectionCount())
            .thenReturn(15L);
        assertEquals(15L, uploadDurationMetrics.getAppUploaderRejectionCount());
        verify(appBinaryStatistics)
            .rejectionCount();
    }

    @Test
    void testGetAppUploaderRejectionCountInWindow() {
        when(appBinaryStatistics.rejectionsInWindow())
            .thenReturn(16L);
        assertEquals(16L, uploadDurationMetrics.getAppUploaderRejectionCountInWindow());
        verify(appBinaryStatistics)
            .rejectionsInWindow();
    }

    @Test
    void testGetObjectStoreUploadTotalCount() {
        when(objectStoreStatistics.totalCount())
            .thenReturn(21L);
        assertEquals(21L, uploadDurationMetrics.getObjectStoreUploadTotalCount());
        verify(objectStoreStatistics)
            .totalCount();
    }

    @Test
    void testGetObjectStoreUploadTimeoutCount() {
        when(objectStoreStatistics.timeoutCount())
            .thenReturn(22L);
        assertEquals(22L, uploadDurationMetrics.getObjectStoreUploadTimeoutCount());
        verify(objectStoreStatistics)
            .timeoutCount();
    }

    @Test
    void testGetObjectStoreUploadMaxDurationMs() {
        when(objectStoreStatistics.maxDurationMs())
            .thenReturn(23L);
        assertEquals(23L, uploadDurationMetrics.getObjectStoreUploadMaxDurationMs());
        verify(objectStoreStatistics)
            .maxDurationMs();
    }

    @Test
    void testGetObjectStoreUploadSumDurationMs() {
        when(objectStoreStatistics.sumDurationMs())
            .thenReturn(24L);
        assertEquals(24L, uploadDurationMetrics.getObjectStoreUploadSumDurationMs());
        verify(objectStoreStatistics)
            .sumDurationMs();
    }

    @Test
    void testGetAppBinaryUploadTimeoutsInWindow() {
        when(appBinaryStatistics.timeoutsInWindow())
            .thenReturn(17L);
        assertEquals(17L, uploadDurationMetrics.getAppBinaryUploadTimeoutsInWindow());
        verify(appBinaryStatistics)
            .timeoutsInWindow();
    }

    @Test
    void testGetObjectStoreUploadTimeoutsInWindow() {
        when(objectStoreStatistics.timeoutsInWindow())
            .thenReturn(25L);
        assertEquals(25L, uploadDurationMetrics.getObjectStoreUploadTimeoutsInWindow());
        verify(objectStoreStatistics)
            .timeoutsInWindow();
    }

}
