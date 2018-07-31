package com.sap.cloud.lm.sl.cf.web.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.jobexecutor.DefaultJobExecutor;
import org.activiti.engine.impl.jobexecutor.FailedJobCommandFactory;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.AbortFailedProcessCommandFactory;

@Configuration
@Profile("cf")
public class ActivitiConfiguration {

    private static final String DATABASE_SCHEMA_UPDATE = "true";

    private static final int JOB_EXECUTOR_QUEUE_SIZE = 16;
    private static final int JOB_EXECUTOR_CORE_POOL_SIZE = 8;
    private static final int JOB_EXECUTOR_MAX_POOL_SIZE = 32;
    private static final int JOB_EXECUTOR_LOCK_TIME_IN_MILLIS = (int) TimeUnit.MINUTES.toMillis(30);
    private static final String JOB_EXECUTOR_ID_TEMPLATE = "ds-%s/%d/%s";

    @Value("classpath*:/com/sap/cloud/lm/sl/cf/process/*.bpmn")
    private Resource[] activitiResources;
    protected Supplier<String> randomIdGenerator = () -> UUID.randomUUID()
        .toString();

    @Inject
    @Bean
    public ProcessEngine processEngine(ApplicationContext applicationContext, SpringProcessEngineConfiguration processEngineConfiguration)
        throws Exception {
        ProcessEngineFactoryBean processEngineFactoryBean = new ProcessEngineFactoryBean();
        processEngineFactoryBean.setApplicationContext(applicationContext);
        processEngineFactoryBean.setProcessEngineConfiguration(processEngineConfiguration);
        return processEngineFactoryBean.getObject();
    }

    @Inject
    @Bean
    public SpringProcessEngineConfiguration processEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
        JobExecutor jobExecutor) {
        SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
        processEngineConfiguration.setDatabaseSchemaUpdate(DATABASE_SCHEMA_UPDATE);
        processEngineConfiguration.setDataSource(dataSource);
        processEngineConfiguration.setTransactionManager(transactionManager);

        processEngineConfiguration.setDeploymentResources(getActivitiResources());
        processEngineConfiguration.setFailedJobCommandFactory(getFailedJobCommandFactory());
        processEngineConfiguration.setJobExecutor(jobExecutor);
        return processEngineConfiguration;
    }

    private Resource[] getActivitiResources() {
        return getActivitiResourcesAsList().toArray(new Resource[0]);
    }

    protected List<Resource> getActivitiResourcesAsList() {
        List<Resource> resources = new ArrayList<>();
        resources.addAll(Arrays.asList(activitiResources));
        return resources;
    }

    protected FailedJobCommandFactory getFailedJobCommandFactory() {
        // By default Activiti will retry failed jobs. Disable this behavior.
        return new AbortFailedProcessCommandFactory();
    }

    @Inject
    @Bean
    public JobExecutor jobExecutor(String jobExecutorId) {
        DefaultJobExecutor jobExecutor = new DefaultJobExecutor();
        scale(jobExecutor);
        jobExecutor.setLockTimeInMillis(JOB_EXECUTOR_LOCK_TIME_IN_MILLIS);
        jobExecutor.setLockOwner(jobExecutorId);
        return jobExecutor;
    }

    protected void scale(DefaultJobExecutor jobExecutor) {
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
