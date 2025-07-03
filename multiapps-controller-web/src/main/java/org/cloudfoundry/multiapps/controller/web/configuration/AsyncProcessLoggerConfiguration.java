package org.cloudfoundry.multiapps.controller.web.configuration;

import jakarta.inject.Inject;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

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

}
