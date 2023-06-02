package org.cloudfoundry.multiapps.controller.web.configuration;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class SpringSchedulingConfiguration {

    private final ApplicationConfiguration applicationConfiguration;

    @Inject
    public SpringSchedulingConfiguration(ApplicationConfiguration applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(applicationConfiguration.getSpringSchedulerTaskExecutorThreads());
        threadPoolTaskScheduler.setThreadNamePrefix("thread-pool-task-scheduler-");
        return threadPoolTaskScheduler;
    }

}
