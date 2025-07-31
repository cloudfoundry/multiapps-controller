package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class PollStartLiveAppExecutionTest {

    private static final String MODULE_NAME = "app-to-process-module";
    private static final String APP_TO_PROCESS_NAME = "app-to-process-live";

    @Mock
    private CloudControllerClientFactory clientFactory;
    @Mock
    private TokenService tokenService;
    @Mock
    private ProcessContext context;

    private PollStartLiveAppExecution pollStartLiveAppExecution;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        pollStartLiveAppExecution = new PollStartLiveAppExecution(clientFactory, tokenService);
    }

    @Test
    void getAppToPollWhenApplicationIsDetected() {
        prepareDeployedMta();
        prepareAppToProcess(APP_TO_PROCESS_NAME, MODULE_NAME);
        CloudApplication appToPoll = pollStartLiveAppExecution.getAppToPoll(context);
        assertEquals(APP_TO_PROCESS_NAME, appToPoll.getName());
        assertEquals(appToPoll, pollStartLiveAppExecution.getApplication(context, APP_TO_PROCESS_NAME, null));
    }

    @Test
    void getAppToPollWhenApplicationIsNotDetected() {
        prepareDeployedMta();
        prepareAppToProcess(APP_TO_PROCESS_NAME, "another-module");
        Exception exception = assertThrows(SLException.class, () -> pollStartLiveAppExecution.getAppToPoll(context));
        assertEquals("Required application to poll: \"app-to-process-live\" not found", exception.getMessage());
    }

    void prepareDeployedMta() {
        DeployedMtaApplication deployedMtaApplication = ImmutableDeployedMtaApplication.builder()
                                                                                       .moduleName(MODULE_NAME)
                                                                                       .name(APP_TO_PROCESS_NAME)
                                                                                       .build();
        DeployedMta deployedMta = ImmutableDeployedMta.builder()
                                                      .metadata(ImmutableMtaMetadata.builder()
                                                                                    .id(UUID.randomUUID()
                                                                                            .toString())
                                                                                    .build())
                                                      .applications(List.of(deployedMtaApplication))
                                                      .build();
        when(context.getVariable(Variables.DEPLOYED_MTA)).thenReturn(deployedMta);
    }

    private void prepareAppToProcess(String appName, String moduleName) {
        CloudApplicationExtended cloudApplicationExtended = ImmutableCloudApplicationExtended.builder()
                                                                                             .name(appName)
                                                                                             .moduleName(moduleName)
                                                                                             .build();
        when(context.getVariable(Variables.APP_TO_PROCESS)).thenReturn(cloudApplicationExtended);
    }

}
