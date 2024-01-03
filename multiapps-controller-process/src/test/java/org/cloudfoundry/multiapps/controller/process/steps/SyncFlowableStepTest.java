package org.cloudfoundry.multiapps.controller.process.steps;

import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersistenceService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersister;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.util.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.util.ProcessHelper;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.job.api.DeadLetterJobQuery;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import org.springframework.http.HttpStatus;

public abstract class SyncFlowableStepTest<T extends SyncFlowableStep> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncFlowableStepTest.class);

    protected static final String USER_NAME = "dummy";
    protected static final String ORG_NAME = "org";
    protected static final String SPACE_NAME = "space";
    protected static final String SPACE_GUID = "spaceGuid";
    protected final String TEST_CORRELATION_ID = "test";
    protected final String TEST_TASK_ID = "testTask";
    protected final String RETRY_STEP_EXECUTION_STATUS = "RETRY";
    protected final String DONE_STEP_EXECUTION_STATUS = "DONE";
    protected static final String SERVICE_NAME = "test-service";
    private static final String METADATA_LABEL = "test-label";
    private static final String METADATA_LABEL_VALUE = "test-label-value";
    private static final String SYSLOG_DRAIN_URL = "test-syslog-url";

    protected final Tester tester = Tester.forClass(getClass());

    protected final DelegateExecution execution = MockDelegateExecution.createSpyInstance();
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
    protected final ProcessLoggerProvider processLoggerProvider = Mockito.spy(ProcessLoggerProvider.class);
    @Mock
    protected ProcessHelper processHelper;
    @InjectMocks
    protected ProcessLogsPersister processLogsPersister = Mockito.spy(ProcessLogsPersister.class);

    protected ProcessContext context;
    @InjectMocks
    protected T step = createStep();

    protected abstract T createStep();

    @BeforeEach
    public void initMocks() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        this.stepLogger = Mockito.spy(new StepLogger(execution, progressMessageService, processLoggerProvider, LOGGER));
        this.context = step.createProcessContext(execution);
        when(stepLoggerFactory.create(any(), any(), any(), any())).thenReturn(stepLogger);
        context.setVariable(Variables.SPACE_NAME, SPACE_NAME);
        context.setVariable(Variables.SPACE_GUID, SPACE_GUID);
        context.setVariable(Variables.USER, USER_NAME);
        context.setVariable(Variables.ORGANIZATION_NAME, ORG_NAME);
        when(clientProvider.getControllerClient(any(), any(), any())).thenReturn(client);
        execution.setVariable("correlationId", getCorrelationId());
        execution.setVariable("__TASK_ID", getTaskId());
        prepareProcessEngineConfiguration();
        context.setVariable(Variables.MODULE_TO_DEPLOY, Module.createV3()
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
        when(mockExecutionQuery.list()).thenReturn(List.of(mockExecution));
        when(mockExecutionQuery.processInstanceId(Mockito.anyString())).thenReturn(mockExecutionQuery);
    }

    private ExecutionQuery createExecutionQueryMock() {
        RuntimeService mockRuntimeService = Mockito.mock(RuntimeService.class);
        when(processEngineConfiguration.getRuntimeService()).thenReturn(mockRuntimeService);
        ExecutionQuery mockExecutionQuery = Mockito.mock(ExecutionQuery.class);
        when(mockRuntimeService.createExecutionQuery()).thenReturn(mockExecutionQuery);
        return mockExecutionQuery;
    }

    protected void assertExecutionStepStatus(String executionStepStatus) {
        assertEquals(executionStepStatus, getExecutionStatus());
    }

    protected void prepareServiceToProcess(CloudServiceInstanceExtended serviceToProcess) {
        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceToProcess);
    }

    protected void prepareClient(CloudServiceInstanceExtended serviceToProcess) {
        when(client.getRequiredServiceInstanceGuid(SERVICE_NAME)).thenReturn(serviceToProcess.getGuid());
    }

    protected CloudServiceInstanceExtended buildServiceToProcess(boolean isOptional) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_NAME)
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .guid(UUID.randomUUID())
                                                                                    .build())
                                                    .syslogDrainUrl(SYSLOG_DRAIN_URL)
                                                    .v3Metadata(Metadata.builder()
                                                                        .label(METADATA_LABEL, METADATA_LABEL_VALUE)
                                                                        .build())
                                                    .isOptional(isOptional)
                                                    .build();
    }

    protected void assertStepFinishedSuccessfully() {
        assertEquals(StepPhase.DONE.toString(), getExecutionStatus());
    }

    protected String getExecutionStatus() {
        return (String) execution.getVariable("StepExecution");
    }

    protected String getCorrelationId() {
        return TEST_CORRELATION_ID;
    }

    private String getTaskId() {
        return TEST_TASK_ID;
    }

}
