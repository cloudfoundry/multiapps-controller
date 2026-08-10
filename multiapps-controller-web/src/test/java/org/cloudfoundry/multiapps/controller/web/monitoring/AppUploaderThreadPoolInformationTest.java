package org.cloudfoundry.multiapps.controller.web.monitoring;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppUploaderThreadPoolInformationTest {

    private ThreadPoolExecutor threadPoolExecutor;
    private AppUploaderThreadPoolInformation appUploaderThreadPoolInformation;

    @BeforeEach
    void setUp() {
        threadPoolExecutor = new ThreadPoolExecutor(2, 5, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        appUploaderThreadPoolInformation = new AppUploaderThreadPoolInformation(threadPoolExecutor);
    }

    @AfterEach
    void cleanUp() {
        threadPoolExecutor.shutdownNow();
    }

    @Test
    void testGetActiveThreadsReturnsActiveCount() {
        assertEquals(threadPoolExecutor.getActiveCount(), appUploaderThreadPoolInformation.getActiveThreads());
    }

    @Test
    void testGetMaxThreadsReturnsMaximumPoolSize() {
        assertEquals(5, appUploaderThreadPoolInformation.getMaxThreads());
        assertEquals(threadPoolExecutor.getMaximumPoolSize(), appUploaderThreadPoolInformation.getMaxThreads());
    }

}
