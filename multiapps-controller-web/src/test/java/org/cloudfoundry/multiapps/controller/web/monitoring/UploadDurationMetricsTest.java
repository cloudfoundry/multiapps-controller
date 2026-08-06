package org.cloudfoundry.multiapps.controller.web.monitoring;

import org.cloudfoundry.multiapps.controller.persistence.monitoring.UploadDurationTracker;
import org.cloudfoundry.multiapps.controller.persistence.monitoring.UploadDurationTracker.UploadPathStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        Mockito.when(uploadDurationTracker.getAppBinaryStatistics())
               .thenReturn(appBinaryStatistics);
        Mockito.when(uploadDurationTracker.getObjectStoreStatistics())
               .thenReturn(objectStoreStatistics);
        uploadDurationMetrics = new UploadDurationMetrics(uploadDurationTracker, appUploaderInfo);
    }

    @Test
    void testGetAppBinaryUploadTotalCount() {
        Mockito.when(appBinaryStatistics.totalCount())
               .thenReturn(11L);
        assertEquals(11L, uploadDurationMetrics.getAppBinaryUploadTotalCount());
        Mockito.verify(appBinaryStatistics)
               .totalCount();
    }

    @Test
    void testGetAppBinaryUploadTimeoutCount() {
        Mockito.when(appBinaryStatistics.timeoutCount())
               .thenReturn(12L);
        assertEquals(12L, uploadDurationMetrics.getAppBinaryUploadTimeoutCount());
        Mockito.verify(appBinaryStatistics)
               .timeoutCount();
    }

    @Test
    void testGetAppBinaryUploadMaxDurationMs() {
        Mockito.when(appBinaryStatistics.maxDurationMs())
               .thenReturn(13L);
        assertEquals(13L, uploadDurationMetrics.getAppBinaryUploadMaxDurationMs());
        Mockito.verify(appBinaryStatistics)
               .maxDurationMs();
    }

    @Test
    void testGetAppBinaryUploadSumDurationMs() {
        Mockito.when(appBinaryStatistics.sumDurationMs())
               .thenReturn(14L);
        assertEquals(14L, uploadDurationMetrics.getAppBinaryUploadSumDurationMs());
        Mockito.verify(appBinaryStatistics)
               .sumDurationMs();
    }

    @Test
    void testGetAppUploaderActiveThreads() {
        Mockito.when(appUploaderInfo.getActiveThreads())
               .thenReturn(3);
        assertEquals(3, uploadDurationMetrics.getAppUploaderActiveThreads());
        Mockito.verify(appUploaderInfo)
               .getActiveThreads();
    }

    @Test
    void testGetAppUploaderMaxThreads() {
        Mockito.when(appUploaderInfo.getMaxThreads())
               .thenReturn(7);
        assertEquals(7, uploadDurationMetrics.getAppUploaderMaxThreads());
        Mockito.verify(appUploaderInfo)
               .getMaxThreads();
    }

    @Test
    void testGetAppUploaderRejectionCount() {
        Mockito.when(appBinaryStatistics.rejectionCount())
               .thenReturn(15L);
        assertEquals(15L, uploadDurationMetrics.getAppUploaderRejectionCount());
        Mockito.verify(appBinaryStatistics)
               .rejectionCount();
    }

    @Test
    void testGetAppUploaderRejectionCountInWindow() {
        Mockito.when(appBinaryStatistics.rejectionsInWindow())
               .thenReturn(16L);
        assertEquals(16L, uploadDurationMetrics.getAppUploaderRejectionCountInWindow());
        Mockito.verify(appBinaryStatistics)
               .rejectionsInWindow();
    }

    @Test
    void testGetObjectStoreUploadTotalCount() {
        Mockito.when(objectStoreStatistics.totalCount())
               .thenReturn(21L);
        assertEquals(21L, uploadDurationMetrics.getObjectStoreUploadTotalCount());
        Mockito.verify(objectStoreStatistics)
               .totalCount();
    }

    @Test
    void testGetObjectStoreUploadTimeoutCount() {
        Mockito.when(objectStoreStatistics.timeoutCount())
               .thenReturn(22L);
        assertEquals(22L, uploadDurationMetrics.getObjectStoreUploadTimeoutCount());
        Mockito.verify(objectStoreStatistics)
               .timeoutCount();
    }

    @Test
    void testGetObjectStoreUploadMaxDurationMs() {
        Mockito.when(objectStoreStatistics.maxDurationMs())
               .thenReturn(23L);
        assertEquals(23L, uploadDurationMetrics.getObjectStoreUploadMaxDurationMs());
        Mockito.verify(objectStoreStatistics)
               .maxDurationMs();
    }

    @Test
    void testGetObjectStoreUploadSumDurationMs() {
        Mockito.when(objectStoreStatistics.sumDurationMs())
               .thenReturn(24L);
        assertEquals(24L, uploadDurationMetrics.getObjectStoreUploadSumDurationMs());
        Mockito.verify(objectStoreStatistics)
               .sumDurationMs();
    }

    @Test
    void testGetAppBinaryUploadTimeoutsInWindow() {
        Mockito.when(appBinaryStatistics.timeoutsInWindow())
               .thenReturn(17L);
        assertEquals(17L, uploadDurationMetrics.getAppBinaryUploadTimeoutsInWindow());
        Mockito.verify(appBinaryStatistics)
               .timeoutsInWindow();
    }

    @Test
    void testGetObjectStoreUploadTimeoutsInWindow() {
        Mockito.when(objectStoreStatistics.timeoutsInWindow())
               .thenReturn(25L);
        assertEquals(25L, uploadDurationMetrics.getObjectStoreUploadTimeoutsInWindow());
        Mockito.verify(objectStoreStatistics)
               .timeoutsInWindow();
    }

}
