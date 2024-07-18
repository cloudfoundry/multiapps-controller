package org.cloudfoundry.multiapps.controller.process.util;

import jakarta.inject.Inject;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
                                      applicationConfiguration.getThreadsForFileStorageUpload(), 0L, TimeUnit.MILLISECONDS,
                                      fileUploadPriorityBlockingQueue) {

            @Override
            protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                RunnableFuture<T> newTaskFor = super.newTaskFor(callable);
                return new PriorityFuture<>(newTaskFor, ((PriorityCallable<T>) callable).getPriority()
                                                                                        .getValue());
            }
        };
    }

    @Bean(name = "appUploaderThreadPool")
    public ExecutorService appUploaderThreadPool() {
        return new ThreadPoolExecutor(applicationConfiguration.getThreadsForFileUploadToController(),
                                      applicationConfiguration.getThreadsForFileUploadToController(), 0, TimeUnit.MILLISECONDS,
                                      new SynchronousQueue<>(), new ThreadPoolExecutor.AbortPolicy());
    }

}
