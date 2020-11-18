package org.cloudfoundry.multiapps.controller.web.configuration;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.flowable.MtaAsyncJobExecutor;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.FailedJobCommandFactory;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class FlowableConfiguration {

    private static final String DATABASE_SCHEMA_UPDATE = "true";

    private static final int ASYNC_JOB_ACQUIRE_WAIT_TIME_IN_MILLIS = (int) TimeUnit.SECONDS.toMillis(3);
    private static final int JOB_EXECUTOR_LOCK_TIME_IN_MILLIS = (int) TimeUnit.MINUTES.toMillis(30);
    private static final long JOB_EXECUTOR_SHUTDOWN_AWAIT_TIME_IN_SECONDS = TimeUnit.MINUTES.toSeconds(8);
    private static final String JOB_EXECUTOR_ID_TEMPLATE = "ds-%s/%d/%s";

    @Value("classpath*:/org/cloudfoundry/multiapps/controller/process/*.bpmn")
    private Resource[] flowableResources;
    protected Supplier<String> randomIdGenerator = () -> UUID.randomUUID()
                                                             .toString();

    @Inject
    @Bean
    @DependsOn("liquibaseChangelog")
    public ProcessEngine processEngine(ApplicationContext applicationContext, SpringProcessEngineConfiguration processEngineConfiguration)
        throws Exception {
        ProcessEngineFactoryBean processEngineFactoryBean = new ProcessEngineFactoryBean();
        processEngineFactoryBean.setApplicationContext(applicationContext);
        processEngineFactoryBean.setProcessEngineConfiguration(processEngineConfiguration);
        return processEngineFactoryBean.getObject();
    }

    @Inject
    @Bean
    @DependsOn("liquibaseChangelog")
    public SpringProcessEngineConfiguration processEngineConfiguration(DataSource dataSource,
                                                                       PlatformTransactionManager transactionManager,
                                                                       AsyncExecutor jobExecutor,
                                                                       @Lazy FailedJobCommandFactory abortFailedProcessCommandFactory,
                                                                       ApplicationConfiguration configuration) {
        SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
        processEngineConfiguration.setDatabaseSchemaUpdate(DATABASE_SCHEMA_UPDATE);
        processEngineConfiguration.setDataSource(dataSource);
        processEngineConfiguration.setTransactionManager(transactionManager);
        processEngineConfiguration.setDeploymentResources(flowableResources);
        processEngineConfiguration.setFailedJobCommandFactory(abortFailedProcessCommandFactory);
        processEngineConfiguration.setAsyncExecutor(jobExecutor);
        // By default Flowable will retry failed jobs and we don't want that.
        processEngineConfiguration.setAsyncExecutorNumberOfRetries(0);
        // In fact these configurations are for AsyncTaskExecutor
        processEngineConfiguration.setAsyncExecutorThreadPoolQueueSize(configuration.getFlowableJobExecutorQueueCapacity());
        processEngineConfiguration.setAsyncExecutorMaxPoolSize(configuration.getFlowableJobExecutorMaxThreads());
        processEngineConfiguration.setAsyncExecutorCorePoolSize(configuration.getFlowableJobExecutorCoreThreads());
        processEngineConfiguration.setAsyncExecutorSecondsToWaitOnShutdown(JOB_EXECUTOR_SHUTDOWN_AWAIT_TIME_IN_SECONDS);
        processEngineConfiguration.setIdGenerator(new StrongUuidGenerator());
        return processEngineConfiguration;
    }

    @Inject
    @Bean
    public AsyncExecutor jobExecutor(String jobExecutorId) {
        MtaAsyncJobExecutor jobExecutor = new MtaAsyncJobExecutor();
        jobExecutor.setAsyncJobLockTimeInMillis(JOB_EXECUTOR_LOCK_TIME_IN_MILLIS);
        jobExecutor.setLockOwner(jobExecutorId);
        jobExecutor.setUnlockOwnedJobs(true);
        // TODO set to true when root cause of the dead lock is found
        jobExecutor.setUnlockOwnedExecutions(false);
        jobExecutor.setTenantId(AbstractEngineConfiguration.NO_TENANT_ID);
        jobExecutor.setDefaultAsyncJobAcquireWaitTimeInMillis(ASYNC_JOB_ACQUIRE_WAIT_TIME_IN_MILLIS);
        return jobExecutor;
    }

    @Inject
    @Bean
    public String jobExecutorId(ApplicationConfiguration applicationConfiguration) {
        String applicationGuid = applicationConfiguration.getApplicationGuid();
        Integer applicationInstanceIndex = applicationConfiguration.getApplicationInstanceIndex();
        if (applicationGuid == null || applicationInstanceIndex == null) {
            return randomIdGenerator.get();
        }
        return buildJobExecutorId(applicationGuid, applicationInstanceIndex);
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
