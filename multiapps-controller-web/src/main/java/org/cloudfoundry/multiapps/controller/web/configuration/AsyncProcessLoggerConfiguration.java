package org.cloudfoundry.multiapps.controller.web.configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import jakarta.inject.Inject;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncProcessLoggerConfiguration {

    @Inject
    private ApplicationConfiguration configuration;

    @Bean("asyncExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(configuration.getFlowableJobExecutorCoreThreads());
        executor.setMaxPoolSize(configuration.getFlowableJobExecutorMaxThreads());
        executor.setQueueCapacity(configuration.getFlowableJobExecutorQueueCapacity());
        executor.initialize();
        return executor;
    }

    @Bean("cloudLoggingAsyncExecutor")
    public Executor getCloudLoggingAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(configuration.getCloudLoggingExecutorCoreThreads());
        executor.setMaxPoolSize(configuration.getCloudLoggingExecutorMaxThreads());
        executor.setQueueCapacity(configuration.getCloudLoggingExecutorQueueCapacity());
        // Log export is best-effort and must never fail a deployment; when the pool and
        // queue are saturated, run the task on the caller thread instead of aborting it.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("cloud-logging-export-");
        executor.initialize();
        return executor;
    }
}
