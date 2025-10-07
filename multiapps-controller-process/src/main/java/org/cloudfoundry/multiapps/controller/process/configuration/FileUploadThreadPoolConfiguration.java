package org.cloudfoundry.multiapps.controller.process.configuration;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.util.PriorityCallable;
import org.cloudfoundry.multiapps.controller.process.util.PriorityFuture;
import org.cloudfoundry.multiapps.controller.process.util.PriorityFutureComparator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileUploadThreadPoolConfiguration {

    private final ApplicationConfiguration applicationConfiguration;

    @Inject
    public FileUploadThreadPoolConfiguration(ApplicationConfiguration applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
    }

    @Bean("fileUploadPriorityBlockingQueue")
    public PriorityBlockingQueue<Runnable> fileUploadPriorityBlockingQueue() {
        return new PriorityBlockingQueue<>(20, new PriorityFutureComparator());
    }

    @Bean(name = "fileStorageThreadPool")
    public ExecutorService fileStorageThreadPool(PriorityBlockingQueue<Runnable> fileUploadPriorityBlockingQueue) {
        return new ThreadPoolExecutor(applicationConfiguration.getThreadsForFileStorageUpload(),
                                      applicationConfiguration.getThreadsForFileStorageUpload(),
                                      0L,
                                      TimeUnit.MILLISECONDS,
                                      fileUploadPriorityBlockingQueue) {

            @Override
            protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                RunnableFuture<T> newTaskFor = super.newTaskFor(callable);
                return new PriorityFuture<>(newTaskFor,
                                            ((PriorityCallable<T>) callable).getPriority()
                                                                            .getValue());
            }
        };
    }

    @Bean(name = "appUploaderThreadPool")
    public ExecutorService appUploaderThreadPool() {
        return new ThreadPoolExecutor(applicationConfiguration.getThreadsForFileUploadToController(),
                                      applicationConfiguration.getThreadsForFileUploadToController(),
                                      0,
                                      TimeUnit.MILLISECONDS,
                                      new SynchronousQueue<>(),
                                      new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean("fileUploadFromUrlQueue")
    public LinkedBlockingQueue<Runnable> fileUploadFromUrlQueue() {
        return new LinkedBlockingQueue<>(20);
    }

    @Bean(name = "asyncFileUploadExecutor")
    public ExecutorService asyncFileUploadExecutor(LinkedBlockingQueue<Runnable> fileUploadFromUrlQueue) {
        return new ThreadPoolExecutor(5,
                                      applicationConfiguration.getFilesAsyncUploadExecutorMaxThreads(),
                                      30,
                                      TimeUnit.SECONDS,
                                      fileUploadFromUrlQueue);
    }

    @Bean(name = "deployFromUrlExecutor")
    public ExecutorService deployFromUrlExecutor() {
        return new ThreadPoolExecutor(5,
                                      // The max thread count should match the maximum capacity of asyncFileUploadExecutor (queue size + max threads).
                                      // A lower value may cause unnecessary task rejections.
                                      // A higher value may cause job failures when asyncFileUploadExecutor becomes full.
                                      applicationConfiguration.getDeployFromUrlExecutorMaxThreads(),
                                      // As the threads are only updating a row and waiting it is ok to have more threads
                                      30,
                                      TimeUnit.SECONDS,
                                      new SynchronousQueue<>()); // A synchronous queue is used so deploy from url jobs immediately start
        // a new thread that updates the database job entry
    }
}
