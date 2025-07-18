package org.cloudfoundry.multiapps.controller.process.util;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationWaitAfterStopVariableGetterTest {

    private static final String APP_NAME = "app-name";

    @Mock
    private StepLogger stepLogger;
    @Mock
    private CloudControllerClientProvider clientProvider;
    @Mock
    private CloudControllerClient client;

    private ProcessContext context;
    private ApplicationWaitAfterStopVariableGetter delayVariableGetter;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(client.getApplicationEnvironment(Mockito.any(UUID.class)))
               .thenReturn(Collections.emptyMap());
        Mockito.when(clientProvider.getControllerClient(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
               .thenReturn(client);
        context = new ProcessContext(MockDelegateExecution.createSpyInstance(), stepLogger, clientProvider);
        context.setVariable(Variables.USER, "user");
        context.setVariable(Variables.USER_GUID, "123-456-789");
        context.setVariable(Variables.SPACE_GUID, "guid");
        context.setVariable(Variables.CORRELATION_ID, "id");
        delayVariableGetter = new ApplicationWaitAfterStopVariableGetter();
    }

    static Stream<Arguments> testGetDelayFromAppEnv() {
        return Stream.of(Arguments.of("10", Duration.ofSeconds(10)), Arguments.of("invalid-input", Duration.ZERO),
                         Arguments.of(("-10"), Duration.ZERO),
                         Arguments.of("9999", Duration.ofSeconds(ApplicationConfiguration.DEFAULT_MAX_STOP_DELAY_IN_SECONDS)),
                         Arguments.of("", Duration.ZERO));
    }

    @ParameterizedTest
    @MethodSource
    void testGetDelayFromAppEnv(String delayFromEnv, Duration expectedDelay) {
        context.setVariable(Variables.APP_TO_PROCESS, createApplicationWithDelayAfterStop(delayFromEnv));
        assertEquals(expectedDelay, delayVariableGetter.getDelayDurationFromAppEnv(context));
    }

    @ParameterizedTest
    @MethodSource("testGetDelayFromAppEnv")
    void testGetDelayFromExistingAppEnv(String delayFromEnv, Duration expectedDelay) {
        context.setVariable(Variables.APP_TO_PROCESS, createApplication());
        var app = createApplicationWithDelayAfterStop(delayFromEnv);
        Mockito.when(client.getApplicationEnvironment(Mockito.eq(app.getGuid())))
               .thenReturn(app.getEnv());
        context.setVariable(Variables.EXISTING_APP, app);
        assertEquals(expectedDelay, delayVariableGetter.getDelayDurationFromAppEnv(context));
    }

    @Test
    void testStopDelayVariableIsSet() {
        context.setVariable(Variables.APP_TO_PROCESS, createApplicationWithDelayAfterStop("1"));
        assertTrue(delayVariableGetter.isAppStopDelayEnvVariableSet(context));
    }

    @Test
    void testStopDelayVariableIsSetFromExistingApp() {
        context.setVariable(Variables.APP_TO_PROCESS, createApplication());
        var app = createApplicationWithDelayAfterStop("1");
        Mockito.when(client.getApplicationEnvironment(Mockito.eq(app.getGuid())))
               .thenReturn(app.getEnv());
        context.setVariable(Variables.EXISTING_APP, app);
        assertTrue(delayVariableGetter.isAppStopDelayEnvVariableSet(context));
    }

    @Test
    void testStopDelayVariableIsNotSet() {
        CloudApplicationExtended appWithNoWaitVar = ImmutableCloudApplicationExtended.builder()
                                                                                     .metadata(ImmutableCloudMetadata.of(UUID.randomUUID()))
                                                                                     .name(APP_NAME)
                                                                                     .build();
        context.setVariable(Variables.APP_TO_PROCESS, appWithNoWaitVar);
        assertFalse(delayVariableGetter.isAppStopDelayEnvVariableSet(context));
    }

    @Test
    void testGetStopDelayVariableReturnsZeroIfNothingSet() {
        context.setVariable(Variables.APP_TO_PROCESS, null);
        context.setVariable(Variables.EXISTING_APP, null);
        assertEquals(Duration.ZERO, delayVariableGetter.getDelayDurationFromAppEnv(context));
    }

    private CloudApplicationExtended createApplication() {
        return ImmutableCloudApplicationExtended.builder()
                                                .metadata(ImmutableCloudMetadata.of(UUID.randomUUID()))
                                                .name(APP_NAME)
                                                .build();
    }

    private CloudApplicationExtended createApplicationWithDelayAfterStop(String waitAfterAppStop) {
        return ImmutableCloudApplicationExtended.builder()
                                                .metadata(ImmutableCloudMetadata.of(UUID.randomUUID()))
                                                .name(APP_NAME)
                                                .env(Map.of(Constants.VAR_WAIT_AFTER_APP_STOP, waitAfterAppStop))
                                                .build();
    }

}
