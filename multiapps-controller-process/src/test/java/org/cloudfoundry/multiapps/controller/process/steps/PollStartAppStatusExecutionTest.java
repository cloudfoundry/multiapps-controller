package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.process.util.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ImmutableInstanceInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableInstancesInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.InstanceInfo;
import com.sap.cloudfoundry.client.facade.domain.InstanceState;
import com.sap.cloudfoundry.client.facade.domain.InstancesInfo;

class PollStartAppStatusExecutionTest {

    private static final String USER_NAME = "testUsername";
    private static final String APP_NAME = "testApplication";
    private static final long PROCESS_START_TIME = LocalDateTime.of(2019, Month.JANUARY, 1, 0, 0)
                                                                .toEpochSecond(ZoneOffset.UTC);

    @Mock
    private StepLogger stepLogger;
    @Mock
    private CloudControllerClientProvider clientProvider;
    @Mock
    private CloudControllerClient client;
    @Mock
    private CloudControllerClientFactory clientFactory;
    @Mock
    private TokenService tokenService;

    private ProcessContext context;
    private PollStartAppStatusExecution step;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        DelegateExecution execution = MockDelegateExecution.createSpyInstance();
        context = new ProcessContext(execution, stepLogger, clientProvider);
        step = new PollStartAppStatusExecution(clientFactory, tokenService);
    }

    static Stream<Arguments> testStep() {
        return Stream.of(Arguments.of(List.of(InstanceState.RUNNING, InstanceState.STARTING), true, AsyncExecutionState.RUNNING),
                         Arguments.of(List.of(InstanceState.CRASHED, InstanceState.CRASHED, InstanceState.CRASHED), true,
                                      AsyncExecutionState.ERROR),
                         Arguments.of(List.of(InstanceState.CRASHED), false, AsyncExecutionState.RUNNING),
                         Arguments.of(List.of(InstanceState.DOWN, InstanceState.RUNNING), true, AsyncExecutionState.ERROR),
                         Arguments.of(List.of(InstanceState.RUNNING, InstanceState.RUNNING), true, AsyncExecutionState.FINISHED));
    }

    @ParameterizedTest
    @MethodSource
    void testStep(List<InstanceState> instancesStates, boolean failOnCrash, AsyncExecutionState expectedAsyncExecutionState) {
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
                                                .metadata(ImmutableCloudMetadata.of(UUID.randomUUID()))
                                                .name(APP_NAME)
                                                .instances(instancesCount)
                                                .build();
    }

    private void prepareContext(CloudApplicationExtended application, boolean failOnCrash) {
        context.setVariable(Variables.USER, USER_NAME);
        context.setVariable(Variables.START_TIME, PROCESS_START_TIME);
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.FAIL_ON_CRASHED, failOnCrash);
    }

    private void prepareClientProvider() {
        when(clientProvider.getControllerClient(any(), any(), any())).thenReturn(client);
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
