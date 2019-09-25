package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.job.api.DeadLetterJobQuery;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersister;
import com.sap.cloud.lm.sl.cf.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.mta.model.Module;

public abstract class SyncFlowableStepTest<T extends SyncFlowableStep> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncFlowableStepTest.class);

    protected static final String USER_NAME = "dummy";
    protected static final String ORG_NAME = "org";
    protected static final String SPACE_NAME = "space";
    protected static final String SPACE_GUID = "spaceGuid";
    protected String TEST_CORRELATION_ID = "test";
    protected String TEST_TASK_ID = "testTask";

    protected Tester tester = Tester.forClass(getClass());

    protected DelegateExecution context = MockDelegateExecution.createSpyInstance();
    @Mock
    protected StepLogger.Factory stepLoggerFactory;
    protected StepLogger stepLogger;
    @Mock
    protected ProcessLogsPersistenceService processLogsPersistenceService;
    @Mock
    protected ProgressMessageService progressMessageService;
    @Mock
    protected FileService fileService;
    @Mock
    protected CloudControllerClient client;
    @Mock
    protected CloudControllerClientProvider clientProvider;
    @Mock
    protected FlowableFacade flowableFacadeFacade;
    @Mock
    protected ApplicationConfiguration configuration;
    @Mock
    protected ProcessEngineConfiguration processEngineConfiguration;
    protected ProcessLoggerProvider processLoggerProvider = Mockito.spy(ProcessLoggerProvider.class);
    @InjectMocks
    protected ProcessLogsPersister processLogsPersister = Mockito.spy(ProcessLogsPersister.class);

    protected ExecutionWrapper execution;
    @InjectMocks
    protected T step = createStep();

    protected abstract T createStep();

    @Before
    @BeforeEach
    public void initMocks() throws FileStorageException {
        MockitoAnnotations.initMocks(this);
        this.stepLogger = Mockito.spy(new StepLogger(context, progressMessageService, processLoggerProvider, LOGGER));
        when(stepLoggerFactory.create(any(), any(), any(), any())).thenReturn(stepLogger);
        context.setVariable(Constants.VAR_SPACE, SPACE_NAME);
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID, SPACE_GUID);
        context.setVariable(Constants.VAR_USER, USER_NAME);
        context.setVariable(Constants.VAR_ORG, ORG_NAME);
        when(clientProvider.getControllerClient(any(), any())).thenReturn(client);
        when(clientProvider.getControllerClient(any(), any(), any(), any())).thenReturn(client);
        context.setVariable("correlationId", getCorrelationId());
        context.setVariable("__TASK_ID", getTaskId());
        prepareExecution();
        prepareProcessEngineConfiguration();
        StepsUtil.setModuleToDeploy(context, Module.createV3()
                                                   .setName("testModule"));
    }

    private void prepareProcessEngineConfiguration() {
        ExecutionQuery mockExecutionQuery = createExecutionQueryMock();
        mockExecutionQuery(mockExecutionQuery);
        DeadLetterJobQuery mockDeadLetterJobQuery = createDeadLetterJobQueryMock();
        mockManagementService(mockDeadLetterJobQuery);
    }

    private void mockManagementService(DeadLetterJobQuery mockDeadLetterJobQuery) {
        ManagementService mockManagementService = Mockito.mock(ManagementService.class);
        when(mockManagementService.createDeadLetterJobQuery()).thenReturn(mockDeadLetterJobQuery);
        when(processEngineConfiguration.getManagementService()).thenReturn(mockManagementService);
    }

    private DeadLetterJobQuery createDeadLetterJobQueryMock() {
        DeadLetterJobQuery mockDeadLetterJobQuery = Mockito.mock(DeadLetterJobQuery.class);
        when(mockDeadLetterJobQuery.processInstanceId(Mockito.anyString())).thenReturn(mockDeadLetterJobQuery);
        when(mockDeadLetterJobQuery.list()).thenReturn(Collections.emptyList());
        return mockDeadLetterJobQuery;
    }

    private void mockExecutionQuery(ExecutionQuery mockExecutionQuery) {
        Execution mockExecution = Mockito.mock(Execution.class);
        when(mockExecution.getActivityId()).thenReturn("1");
        when(mockExecutionQuery.list()).thenReturn(Arrays.asList(mockExecution));
        when(mockExecutionQuery.processInstanceId(Mockito.anyString())).thenReturn(mockExecutionQuery);
    }

    private ExecutionQuery createExecutionQueryMock() {
        RuntimeService mockRuntimeService = Mockito.mock(RuntimeService.class);
        when(processEngineConfiguration.getRuntimeService()).thenReturn(mockRuntimeService);
        ExecutionQuery mockExecutionQuery = Mockito.mock(ExecutionQuery.class);
        when(mockRuntimeService.createExecutionQuery()).thenReturn(mockExecutionQuery);
        return mockExecutionQuery;
    }

    private void prepareExecution() {
        execution = step.createExecutionWrapper(context);
    }

    protected void assertStepFinishedSuccessfully() {
        assertEquals(StepPhase.DONE.toString(), getExecutionStatus());
    }

    protected String getExecutionStatus() {
        return (String) context.getVariable("StepExecution");
    }

    protected String getCorrelationId() {
        return TEST_CORRELATION_ID;
    }

    private String getTaskId() {
        return TEST_TASK_ID;
    }

}
