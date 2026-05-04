package org.cloudfoundry.multiapps.controller.process.util;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadinessHealthCheckUtilTest {

    @Mock
    private StepLogger stepLogger;
    @Mock
    private CloudControllerClientProvider clientProvider;

    static Stream<Arguments> testShouldWaitForAppToBecomeRoutable() {
        return Stream.of(
            // (0) Test shouldWaitForAppToBecomeRoutable when enabled is null
            Arguments.of(null, false),
            // (1) Test shouldWaitForAppToBecomeRoutable when enabled is false
            Arguments.of(null, false),
            // (2) Test shouldWaitForAppToBecomeRoutable when enabled is true and type is null
            Arguments.of(null, false),
            // (3) Test shouldWaitForAppToBecomeRoutable when enabled is true and type is not null
            Arguments.of("http", true));
    }

    @ParameterizedTest
    @MethodSource
    void testShouldWaitForAppToBecomeRoutable(String readinessHealthCheckType, boolean expectedResult) {
        assertEquals(expectedResult, ReadinessHealthCheckUtil.shouldWaitForAppToBecomeRoutable(
            createContext(readinessHealthCheckType)));
    }

    private ProcessContext createContext(String readinessHealthCheckType) {
        DelegateExecution execution = MockDelegateExecution.createSpyInstance();
        ProcessContext context = new ProcessContext(execution, stepLogger, clientProvider);

        context.setVariable(Variables.APP_TO_PROCESS, createAppToProcess(readinessHealthCheckType));
        return context;
    }

    private CloudApplicationExtended createAppToProcess(String readinessHealthCheckType) {
        return ImmutableCloudApplicationExtended.builder()
                                                .staging(createStaging(readinessHealthCheckType))
                                                .build();
    }

    private Staging createStaging(String readinessHealthCheckType) {
        return ImmutableStaging.builder()
                               .readinessHealthCheckType(readinessHealthCheckType)
                               .build();
    }
}
