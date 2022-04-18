package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class ApplicationWaitAfterStopHandlerTest {

    private static final String APP_NAME = "app-name";
    private final ProcessContext context = createProcessContext();

    private ApplicationWaitAfterStopVariableGetter delayVariableGetter;
    private ApplicationWaitAfterStopHandler stopHandler;

    @BeforeEach
    public void setup() {
        delayVariableGetter = Mockito.mock(ApplicationWaitAfterStopVariableGetter.class);
        stopHandler = new ApplicationWaitAfterStopHandler(delayVariableGetter);
    }

    static Stream<Arguments> testConfigureDelayAfterAppStop() {
        return Stream.of(Arguments.of(true, Duration.ofSeconds(1)), Arguments.of(true, Duration.ofSeconds(1)),
                         Arguments.of(false, Duration.ofSeconds(0)), Arguments.of(true, Duration.ofSeconds(0)));
    }

    @ParameterizedTest
    @MethodSource
    void testConfigureDelayAfterAppStop(boolean shouldWaitAfterStop, Duration durationToWait) {
        Mockito.when(delayVariableGetter.isAppStopDelayEnvVariableSet(context))
               .thenReturn(shouldWaitAfterStop);
        Mockito.when(delayVariableGetter.getDelayDurationFromAppEnv(context))
               .thenReturn(durationToWait);

        stopHandler.configureDelayAfterAppStop(context, APP_NAME);

        assertEquals(durationToWait, context.getVariable(Variables.DELAY_AFTER_APP_STOP));
    }

    private ProcessContext createProcessContext() {
        return new ProcessContext(MockDelegateExecution.createSpyInstance(),
                                  Mockito.mock(StepLogger.class),
                                  Mockito.mock(CloudControllerClientProvider.class));
    }

}
