package org.cloudfoundry.multiapps.controller.web.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.BlockingQueue;

import org.flowable.common.engine.impl.async.DefaultAsyncTaskExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FlowableJobExecutorInformationTest {

    private static final int JOBS_IN_QUEUE = 5;
    private static final int UPDATED_JOBS_IN_QUEUE = 1;

    @Mock
    private DefaultAsyncTaskExecutor asyncExecutor;
    @Mock
    private BlockingQueue<Runnable> blockingQueue;

    @InjectMocks
    private FlowableJobExecutorInformation flowableJobExecutorInformation;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testGetCurrentJobExecutorQueueSize() {
        prepareJobExecutor(JOBS_IN_QUEUE);

        int currentJobExecutorQueueSize = flowableJobExecutorInformation.getCurrentJobExecutorQueueSize();
        assertEquals(JOBS_IN_QUEUE, currentJobExecutorQueueSize);
    }

    private void prepareJobExecutor(int jobsInQueue) {
        when(blockingQueue.size()).thenReturn(jobsInQueue);
        when(asyncExecutor.getThreadPoolQueue()).thenReturn(blockingQueue);
    }

    @Test
    void testGetCurrentJobExecutorQueueSizeFromCache() {
        prepareJobExecutor(JOBS_IN_QUEUE);
        flowableJobExecutorInformation.getCurrentJobExecutorQueueSize();

        prepareJobExecutor(UPDATED_JOBS_IN_QUEUE);
        int currentJobExecutorQueueSize = flowableJobExecutorInformation.getCurrentJobExecutorQueueSize();

        assertEquals(JOBS_IN_QUEUE, currentJobExecutorQueueSize);
    }

    @Test
    void testGetCurrentJobExecutorQueueSizeExpiredCache() {
        flowableJobExecutorInformation = new FlowableJobExecutorInformation(asyncExecutor) {
            @Override
            protected void updateTime() {
                lastUpdateTime = Instant.now()
                                        .minusSeconds(120);
            }
        };
        prepareJobExecutor(JOBS_IN_QUEUE);
        flowableJobExecutorInformation.getCurrentJobExecutorQueueSize();

        prepareJobExecutor(UPDATED_JOBS_IN_QUEUE);
        int currentJobExecutorQueueSize = flowableJobExecutorInformation.getCurrentJobExecutorQueueSize();

        assertEquals(UPDATED_JOBS_IN_QUEUE, currentJobExecutorQueueSize);
    }

}
