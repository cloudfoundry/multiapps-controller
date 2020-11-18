package org.cloudfoundry.multiapps.controller.process.flowable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.job.api.DeadLetterJobQuery;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class FlowableFacadeTest {

    private static final Supplier<String> UUID_SUPPLIER = () -> UUID.randomUUID()
                                                                    .toString();
    private static final String PROCESS_INSTANCE_ID = UUID_SUPPLIER.get();
    private static final String JOB_ID = UUID_SUPPLIER.get();

    private FlowableFacade flowableFacade;

    @Mock
    private MtaAsyncJobExecutor mockedAsyncExecutor;
    @Mock
    private ProcessEngine mockedProcessEngine;
    @Mock
    private ProcessEngineConfiguration mockedProcessEngineConfiguration;
    @Mock
    private RuntimeService mockedRuntimeService;
    @Mock
    private ExecutionQuery mockedExecutionQuery;
    @Mock
    private ManagementService mockedManagementService;
    @Mock
    private DeadLetterJobQuery mockedDeadLetterJobQuery;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();

        Mockito.when(mockedProcessEngineConfiguration.getAsyncExecutor())
               .thenReturn(mockedAsyncExecutor);
        Mockito.when(mockedProcessEngine.getProcessEngineConfiguration())
               .thenReturn(mockedProcessEngineConfiguration);
        Mockito.when(mockedProcessEngine.getRuntimeService())
               .thenReturn(mockedRuntimeService);
        Mockito.when(mockedRuntimeService.createExecutionQuery())
               .thenReturn(mockedExecutionQuery);
        Mockito.when(mockedProcessEngine.getManagementService())
               .thenReturn(mockedManagementService);
        Mockito.when(mockedManagementService.createDeadLetterJobQuery())
               .thenReturn(mockedDeadLetterJobQuery);

        flowableFacade = new FlowableFacade(mockedProcessEngine);
    }

    @Test
    void testAsyncExecutorMethodsAreCalled() {
        flowableFacade.shutdownJobExecutor();
        Mockito.verify(mockedAsyncExecutor, Mockito.times(1))
               .shutdown();
    }

    @Test
    void testExecuteJobWithDuplicatedJobIds() {
        mockProcessExecutions(PROCESS_INSTANCE_ID, PROCESS_INSTANCE_ID);
        mockDeadLetterJobQuery(PROCESS_INSTANCE_ID, JOB_ID);
        flowableFacade.executeJob(UUID_SUPPLIER.get());
        Mockito.verify(mockedManagementService)
               .moveDeadLetterJobToExecutableJob(JOB_ID, 0);
    }

    @Test
    void testExecuteJobWithoutDuplicatedJobIds() {
        String firstProcessInstanceId = UUID_SUPPLIER.get();
        String firstJobId = UUID_SUPPLIER.get();
        String secondJobId = UUID_SUPPLIER.get();
        mockProcessExecutions(firstProcessInstanceId);
        mockDeadLetterJobQuery(firstProcessInstanceId, firstJobId, secondJobId);
        flowableFacade.executeJob(UUID_SUPPLIER.get());
        Mockito.verify(mockedManagementService)
               .moveDeadLetterJobToExecutableJob(firstJobId, 0);
        Mockito.verify(mockedManagementService)
               .moveDeadLetterJobToExecutableJob(secondJobId, 0);
    }

    @Test
    void testExecuteJobWhenFlowableObjectIsNotFound() {
        mockProcessExecutions(PROCESS_INSTANCE_ID);
        mockDeadLetterJobQuery(PROCESS_INSTANCE_ID, JOB_ID);
        Mockito.when(mockedManagementService.moveDeadLetterJobToExecutableJob(anyString(), anyInt()))
               .thenThrow(FlowableObjectNotFoundException.class);
        assertDoesNotThrow(() -> flowableFacade.executeJob(UUID_SUPPLIER.get()));
    }

    private void mockProcessExecutions(String... processInstanceIds) {
        Mockito.when(mockedExecutionQuery.rootProcessInstanceId(anyString()))
               .thenReturn(mockedExecutionQuery);
        List<Execution> mockedExecution = toList(this::createMockedExecution, processInstanceIds);
        Mockito.when(mockedExecutionQuery.list())
               .thenReturn(mockedExecution);
    }

    private <T> List<T> toList(Function<? super String, ? extends T> mapper, String... ids) {
        return Arrays.stream(ids)
                     .map(mapper)
                     .collect(Collectors.toList());
    }

    private Execution createMockedExecution(String processInstanceId) {
        Execution execution = Mockito.mock(Execution.class);
        Mockito.when(execution.getProcessInstanceId())
               .thenReturn(processInstanceId);
        return execution;
    }

    private void mockDeadLetterJobQuery(String processInstanceId, String... jobIds) {
        Mockito.when(mockedDeadLetterJobQuery.processInstanceId(processInstanceId))
               .thenReturn(mockedDeadLetterJobQuery);
        List<Job> mockedJobs = toList(this::createMockedJob, jobIds);
        Mockito.when(mockedDeadLetterJobQuery.list())
               .thenReturn(mockedJobs);
    }

    private Job createMockedJob(String jobId) {
        Job job = Mockito.mock(Job.class);
        Mockito.when(job.getId())
               .thenReturn(jobId);
        return job;
    }

}
