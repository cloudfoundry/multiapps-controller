package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationStager;
import com.sap.cloud.lm.sl.cf.process.util.StagingState;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;

public class PollStageAppStatusExecutionTest {

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
    private ProcessLoggerProvider processLoggerProvider;
    @Mock
    private CloudControllerClientProvider clientProvider;
    @Mock
    private CloudControllerClient client;

    private ExecutionWrapper executionWrapper;
    private DelegateExecution context;
    private PollStageAppStatusExecution step;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = MockDelegateExecution.createSpyInstance();
        executionWrapper = new ExecutionWrapper(context, stepLogger, clientProvider);
        step = new PollStageAppStatusExecution(recentLogsRetriever, applicationStager);
    }

    public static Stream<Arguments> testStep() {
        return Stream.of(
        //@formatter:off
                        Arguments.of(PackageState.FAILED, AsyncExecutionState.ERROR),
                        Arguments.of(PackageState.PENDING, AsyncExecutionState.RUNNING),
                        Arguments.of(PackageState.STAGED, AsyncExecutionState.FINISHED)
        //@formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStep(PackageState applicationStageState, AsyncExecutionState expectedExecutionState) {
        CloudApplicationExtended application = buildApplication();
        prepareContext(application);
        prepareClientProvider();
        prepareClient(application);
        StagingState stagingState = buildStagingState(applicationStageState);
        prepareApplicationStager(stagingState);

        AsyncExecutionState executionState = step.execute(executionWrapper);

        assertEquals(expectedExecutionState, executionState);
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
        context.setVariable(Constants.VAR_USER, USER_NAME);
        context.setVariable(Constants.VAR_START_TIME, PROCESS_START_TIME);
        StepsUtil.setApp(context, application);
    }

    private void prepareClientProvider() {
        when(clientProvider.getControllerClient(any(), any())).thenReturn(client);
    }

    private void prepareClient(CloudApplicationExtended application) {
        when(client.getApplication(APPLICATION_NAME)).thenReturn(application);

    }

    private StagingState buildStagingState(PackageState applicationStageState) {
        return new StagingState(applicationStageState, "");
    }

    private void prepareApplicationStager(StagingState stagingState) {
        when(applicationStager.getStagingState(any())).thenReturn(stagingState);
    }
}
