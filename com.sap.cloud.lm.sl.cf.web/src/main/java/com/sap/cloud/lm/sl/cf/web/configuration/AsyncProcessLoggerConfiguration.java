package com.sap.cloud.lm.sl.cf.web.configuration;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

@Configuration
@EnableAsync
public class AsyncProcessLoggerConfiguration {

    final ApplicationConfiguration configuration = new ApplicationConfiguration();

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
