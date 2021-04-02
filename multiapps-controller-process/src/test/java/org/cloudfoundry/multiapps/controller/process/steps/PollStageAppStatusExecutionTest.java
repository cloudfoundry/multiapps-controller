package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.cf.clients.RecentLogsRetriever;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationStager;
import org.cloudfoundry.multiapps.controller.process.util.ImmutableStagingState;
import org.cloudfoundry.multiapps.controller.process.util.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.util.StagingState;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.PackageState;

class PollStageAppStatusExecutionTest {

    private static final String USER_NAME = "testUsername";
    private static final String APPLICATION_NAME = "testApplication";
    private static final long PROCESS_START_TIME = new GregorianCalendar(2019, Calendar.JANUARY, 1).toInstant()
                                                                                                   .toEpochMilli();

    @Mock
    private RecentLogsRetriever recentLogsRetriever;
    @Mock
    private ApplicationStager applicationStager;
    @Mock
    private StepLogger stepLogger;
    @Mock
    private CloudControllerClientProvider clientProvider;
    @Mock
    private CloudControllerClient client;

    private ProcessContext context;
    private DelegateExecution execution;
    private PollStageAppStatusExecution step;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        execution = MockDelegateExecution.createSpyInstance();
        context = new ProcessContext(execution, stepLogger, clientProvider);
        step = new PollStageAppStatusExecution(recentLogsRetriever, applicationStager);
    }

    static Stream<Arguments> testStep() {
        return Stream.of(Arguments.of(PackageState.FAILED, AsyncExecutionState.ERROR),
                         Arguments.of(PackageState.PENDING, AsyncExecutionState.RUNNING),
                         Arguments.of(PackageState.STAGED, AsyncExecutionState.FINISHED));
    }

    @ParameterizedTest
    @MethodSource
    void testStep(PackageState applicationStageState, AsyncExecutionState expectedExecutionState) {
        CloudApplicationExtended application = buildApplication();
        prepareContext(application);
        prepareClientProvider();
        prepareClient(application);
        StagingState stagingState = buildStagingState(applicationStageState);
        prepareApplicationStager(stagingState);

        AsyncExecutionState executionState = step.execute(context);

        assertEquals(expectedExecutionState, executionState);
    }

    @Test
    void testPollingErrorMessage() {
        context.setVariable(Variables.APP_TO_PROCESS, createCloudApplication("anatz"));
        String pollingErrorMessage = step.getPollingErrorMessage(context);
        Assertions.assertEquals("Error staging application \"anatz\"", pollingErrorMessage);
    }

    private CloudApplicationExtended createCloudApplication(String appName) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(appName)
                                                .build();
    }

    private CloudApplicationExtended buildApplication() {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APPLICATION_NAME)
                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                .guid(UUID.randomUUID())
                                                                                .build())
                                                .build();
    }

    private void prepareContext(CloudApplicationExtended application) {
        context.setVariable(Variables.USER, USER_NAME);
        context.setVariable(Variables.START_TIME, PROCESS_START_TIME);
        context.setVariable(Variables.APP_TO_PROCESS, application);
    }

    private void prepareClientProvider() {
        when(clientProvider.getControllerClient(any(), any(), any())).thenReturn(client);
    }

    private void prepareClient(CloudApplicationExtended application) {
        when(client.getApplication(APPLICATION_NAME)).thenReturn(application);

    }

    private StagingState buildStagingState(PackageState applicationStageState) {
        return ImmutableStagingState.builder()
                                    .state(applicationStageState)
                                    .build();
    }

    private void prepareApplicationStager(StagingState stagingState) {
        when(applicationStager.getStagingState()).thenReturn(stagingState);
    }

}
