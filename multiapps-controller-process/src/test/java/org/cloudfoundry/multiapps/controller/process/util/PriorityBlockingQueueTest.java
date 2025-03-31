package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.PriorityBlockingQueue;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.configuration.FileUploadThreadPoolConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PriorityBlockingQueueTest {

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testPriorityQueueOrdering() {
        FileUploadThreadPoolConfiguration fileUploadThreadPoolConfiguration = new FileUploadThreadPoolConfiguration(applicationConfiguration);
        PriorityBlockingQueue<Runnable> priorityBlockingQueue = fileUploadThreadPoolConfiguration.fileUploadPriorityBlockingQueue();
        priorityBlockingQueue.offer(new PriorityFuture<>(null, PriorityFuture.Priority.LOWEST.getValue()));
        priorityBlockingQueue.offer(new PriorityFuture<>(null, PriorityFuture.Priority.HIGHEST.getValue()));
        priorityBlockingQueue.offer(new PriorityFuture<>(null, PriorityFuture.Priority.MODERATE.getValue()));
        assertEquals(PriorityFuture.Priority.HIGHEST.getValue(), ((PriorityFuture<Runnable>) priorityBlockingQueue.poll()).getPriority());
        assertEquals(PriorityFuture.Priority.MODERATE.getValue(), ((PriorityFuture<Runnable>) priorityBlockingQueue.poll()).getPriority());
        assertEquals(PriorityFuture.Priority.LOWEST.getValue(), ((PriorityFuture<Runnable>) priorityBlockingQueue.poll()).getPriority());
    }
}
