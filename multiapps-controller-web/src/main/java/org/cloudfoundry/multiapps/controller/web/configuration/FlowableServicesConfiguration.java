package org.cloudfoundry.multiapps.controller.web.configuration;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Configuration for Flowable engine service beans.
 *
 * These beans are extracted from FlowableConfiguration to ensure proper initialization order.
 * The @DependsOn("processEngine") ensures that the ProcessEngineFactoryBean is fully initialized
 * and the ProcessEngine is available before these service beans are created.
 *
 * Note: @Lazy is not used here because:
 * 1. The ProcessEngine is properly managed via ProcessEngineFactoryBean, which handles lifecycle correctly
 * 2. Components like FlowableHistoricDataCleaner need HistoryService at startup for scheduled jobs
 * 3. Using @Lazy could cause unexpected initialization timing issues
 */
@Configuration
public class FlowableServicesConfiguration {

    @Bean
    @DependsOn("processEngine")
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    @DependsOn("processEngine")
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }
}
