package org.cloudfoundry.multiapps.controller.web.configuration;

import org.flowable.engine.ProcessEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;

@Configuration
public class FlowableServicesConfiguration {

    // Mark as @Lazy and @DependsOn to ensure 'processEngine' is produced first,
    // and service beans only resolve when actually needed.

    @Bean
    @Lazy
    @DependsOn("processEngine")
    public org.flowable.engine.RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    @Lazy
    @DependsOn("processEngine")
    public org.flowable.engine.HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }
}
