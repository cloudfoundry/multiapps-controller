package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudControllerClient;
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
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.util.MapUtil;

public class PollStartAppStatusExecutionTest {

    private static final String USER_NAME = "testUsername";
    private static final String APP_NAME = "testApplcaition";
    private static final String INSTANCE_STATE = "state";
    private static final long PROCESS_START_TIME = new GregorianCalendar(2019, Calendar.JANUARY, 1).toInstant()
                                                                                                   .toEpochMilli();

    @Mock
    private RecentLogsRetriever recentLogsRetriever;
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
    private PollStartAppStatusExecution step;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = MockDelegateExecution.createSpyInstance();
        executionWrapper = new ExecutionWrapper(context, stepLogger, clientProvider);
        step = new PollStartAppStatusExecution(recentLogsRetriever);
    }

    public static Stream<Arguments> testStep() {
        return Stream.of(
        //@formatter:off
                        Arguments.of(Arrays.asList(InstanceState.RUNNING, InstanceState.STARTING), true, AsyncExecutionState.RUNNING),
                        Arguments.of(Arrays.asList(InstanceState.CRASHED, InstanceState.CRASHED, InstanceState.CRASHED), true, AsyncExecutionState.ERROR),
                        Arguments.of(Arrays.asList(InstanceState.CRASHED), false, AsyncExecutionState.RUNNING),
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

        AsyncExecutionState executionState = step.execute(executionWrapper);

        assertEquals(expectedAsyncExecutionState, executionState);
    }

    private CloudApplicationExtended buildApplication(int instancesCount) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_NAME)
                                                .instances(instancesCount)
                                                .build();
    }

    private void prepareContext(CloudApplicationExtended application, boolean failOnCrash) {
        context.setVariable(Constants.VAR_USER, USER_NAME);
        context.setVariable(Constants.VAR_START_TIME, PROCESS_START_TIME);
        StepsUtil.setApp(context, application);
        context.setVariable(Constants.PARAM_FAIL_ON_CRASHED, failOnCrash);
    }

    private void prepareClientProvider() {
        when(clientProvider.getControllerClient(any(), any())).thenReturn(client);
    }

    private InstancesInfo buildInstancesInfo(List<InstanceState> instancesStates) {
        List<Map<String, Object>> instancesMap = instancesStates.stream()
                                                                .map(this::addStatesInMap)
                                                                .collect(Collectors.toList());
        return new InstancesInfo(instancesMap);
    }

    private Map<String, Object> addStatesInMap(InstanceState state) {
        return MapUtil.asMap(INSTANCE_STATE, state.toString());
    }

    private void prepareClient(CloudApplicationExtended application, InstancesInfo instancesInfo) {
        when(client.getApplication(anyString())).thenReturn(application);
        when(client.getApplicationInstances(application)).thenReturn(instancesInfo);
    }

}
