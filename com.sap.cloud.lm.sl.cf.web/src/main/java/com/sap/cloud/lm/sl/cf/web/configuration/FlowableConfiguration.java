package com.sap.cloud.lm.sl.cf.web.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.flowable.job.service.impl.asyncexecutor.FailedJobCommandFactory;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.AbortFailedProcessCommandFactory;

@Configuration
@Profile("cf")
public class FlowableConfiguration {

    private static final String DATABASE_SCHEMA_UPDATE = "true";

    private static final int JOB_EXECUTOR_QUEUE_SIZE = 16;
    private static final int JOB_EXECUTOR_CORE_POOL_SIZE = 8;
    private static final int JOB_EXECUTOR_MAX_POOL_SIZE = 32;
    private static final int JOB_EXECUTOR_LOCK_TIME_IN_MILLIS = (int) TimeUnit.MINUTES.toMillis(30);
    private static final String JOB_EXECUTOR_ID_TEMPLATE = "ds-%s/%d/%s";

    @Value("classpath*:/com/sap/cloud/lm/sl/cf/process/*.bpmn")
    private Resource[] flowableResources;
    protected Supplier<String> randomIdGenerator = () -> UUID.randomUUID()
        .toString();

    @Inject
    @Bean
    @DependsOn ("coreChangelog")
    public ProcessEngine processEngine(ApplicationContext applicationContext, SpringProcessEngineConfiguration processEngineConfiguration)
        throws Exception {
        ProcessEngineFactoryBean processEngineFactoryBean = new ProcessEngineFactoryBean();
        processEngineFactoryBean.setApplicationContext(applicationContext);
        processEngineFactoryBean.setProcessEngineConfiguration(processEngineConfiguration);
        return processEngineFactoryBean.getObject();
    }

    @Inject
    @Bean
    @DependsOn ("coreChangelog")
    public SpringProcessEngineConfiguration processEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
        AsyncExecutor jobExecutor) {
        SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
        processEngineConfiguration.setDatabaseSchemaUpdate(DATABASE_SCHEMA_UPDATE);
        processEngineConfiguration.setDataSource(dataSource);
        processEngineConfiguration.setTransactionManager(transactionManager);

        processEngineConfiguration.setDeploymentResources(getFlowableResources());
        processEngineConfiguration.setFailedJobCommandFactory(getFailedJobCommandFactory());
        processEngineConfiguration.setAsyncExecutor(jobExecutor);
        processEngineConfiguration.setAsyncExecutorNumberOfRetries(0);
        return processEngineConfiguration;
    }

    private Resource[] getFlowableResources() {
        return getFlowableResourcesAsList().toArray(new Resource[0]);
    }

    protected List<Resource> getFlowableResourcesAsList() {
        List<Resource> resources = new ArrayList<>();
        resources.addAll(Arrays.asList(flowableResources));
        return resources;
    }

    protected FailedJobCommandFactory getFailedJobCommandFactory() {
        // By default Activiti will retry failed jobs. Disable this behavior.
        return new AbortFailedProcessCommandFactory();
    }

    @Inject
    @Bean
    public AsyncExecutor jobExecutor(String jobExecutorId) {
        DefaultAsyncJobExecutor jobExecutor = new DefaultAsyncJobExecutor();
        scale(jobExecutor);
        jobExecutor.setAsyncJobLockTimeInMillis(JOB_EXECUTOR_LOCK_TIME_IN_MILLIS);
        jobExecutor.setLockOwner(jobExecutorId);
        return jobExecutor;
    }

    protected void scale(DefaultAsyncJobExecutor jobExecutor) {
        jobExecutor.setQueueSize(JOB_EXECUTOR_QUEUE_SIZE);
        jobExecutor.setCorePoolSize(JOB_EXECUTOR_CORE_POOL_SIZE);
        jobExecutor.setMaxPoolSize(JOB_EXECUTOR_MAX_POOL_SIZE);
    }

    @Inject
    @Bean
    public String jobExecutorId(ApplicationConfiguration applicationConfiguration) {
        String applicationId = applicationConfiguration.getApplicationId();
        Integer applicationInstanceIndex = applicationConfiguration.getApplicationInstanceIndex();
        if (applicationId == null || applicationInstanceIndex == null) {
            return randomIdGenerator.get();
        }
        return buildJobExecutorId(applicationId, applicationInstanceIndex);
    }

    private String buildJobExecutorId(String applicationId, int applicationInstanceIndex) {
        return String.format(JOB_EXECUTOR_ID_TEMPLATE, applicationId, applicationInstanceIndex, randomIdGenerator.get());
    }

    @Inject
    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Inject
    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

}
