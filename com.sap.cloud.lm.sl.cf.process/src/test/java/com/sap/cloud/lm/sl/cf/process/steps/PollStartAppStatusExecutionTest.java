package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ImmutableInstanceInfo;
import org.cloudfoundry.client.lib.domain.ImmutableInstancesInfo;
import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
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
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class PollStartAppStatusExecutionTest {

    private static final String USER_NAME = "testUsername";
    private static final String APP_NAME = "testApplcaition";
    private static final long PROCESS_START_TIME = new GregorianCalendar(2019, Calendar.JANUARY, 1).toInstant()
                                                                                                   .toEpochMilli();

    @Mock
    private RecentLogsRetriever recentLogsRetriever;
    @Mock
    private StepLogger stepLogger;
    @Mock
    private CloudControllerClientProvider clientProvider;
    @Mock
    private CloudControllerClient client;

    private ProcessContext context;
    private DelegateExecution execution;
    private PollStartAppStatusExecution step;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        execution = MockDelegateExecution.createSpyInstance();
        context = new ProcessContext(execution, stepLogger, clientProvider);
        step = new PollStartAppStatusExecution(recentLogsRetriever);
    }

    public static Stream<Arguments> testStep() {
        return Stream.of(
        //@formatter:off
                        Arguments.of(Arrays.asList(InstanceState.RUNNING, InstanceState.STARTING), true, AsyncExecutionState.RUNNING),
                        Arguments.of(Arrays.asList(InstanceState.CRASHED, InstanceState.CRASHED, InstanceState.CRASHED), true, AsyncExecutionState.ERROR),
                        Arguments.of(Collections.singletonList(InstanceState.CRASHED), false, AsyncExecutionState.RUNNING),
                        Arguments.of(Arrays.asList(InstanceState.FLAPPING, InstanceState.RUNNING), true, AsyncExecutionState.ERROR),
                        Arguments.of(Arrays.asList(InstanceState.RUNNING, InstanceState.RUNNING), true, AsyncExecutionState.FINISHED)
        );
        //@formatter:on
    }

    @ParameterizedTest
    @MethodSource
    public void testStep(List<InstanceState> instancesStates, boolean failOnCrash, AsyncExecutionState expectedAsyncExecutionState) {
        CloudApplicationExtended application = buildApplication(instancesStates.size());
        prepareContext(application, failOnCrash);
        prepareClientProvider();
        InstancesInfo instancesInfo = buildInstancesInfo(instancesStates);
        prepareClient(application, instancesInfo);

        AsyncExecutionState executionState = step.execute(context);

        assertEquals(expectedAsyncExecutionState, executionState);
    }

    private CloudApplicationExtended buildApplication(int instancesCount) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_NAME)
                                                .instances(instancesCount)
                                                .build();
    }

    private void prepareContext(CloudApplicationExtended application, boolean failOnCrash) {
        execution.setVariable(Constants.VAR_USER, USER_NAME);
        execution.setVariable(Constants.VAR_START_TIME, PROCESS_START_TIME);
        context.setVariable(Variables.APP_TO_PROCESS, application);
        execution.setVariable(Constants.PARAM_FAIL_ON_CRASHED, failOnCrash);
    }

    private void prepareClientProvider() {
        when(clientProvider.getControllerClient(any(), any())).thenReturn(client);
    }

    private InstancesInfo buildInstancesInfo(List<InstanceState> instancesStates) {
        List<InstanceInfo> instances = instancesStates.stream()
                                                      .map(this::buildInstanceInfo)
                                                      .collect(Collectors.toList());
        return ImmutableInstancesInfo.builder()
                                     .instances(instances)
                                     .build();
    }

    private InstanceInfo buildInstanceInfo(InstanceState state) {
        return ImmutableInstanceInfo.builder()
                                    .index(0)
                                    .state(state)
                                    .build();
    }

    private void prepareClient(CloudApplicationExtended application, InstancesInfo instancesInfo) {
        when(client.getApplication(anyString())).thenReturn(application);
        when(client.getApplicationInstances(application)).thenReturn(instancesInfo);
    }

}
