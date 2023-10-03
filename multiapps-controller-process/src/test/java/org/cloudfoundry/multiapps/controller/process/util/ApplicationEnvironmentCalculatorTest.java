package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;

class ApplicationEnvironmentCalculatorTest {

    private static final String APP_NAME = "anatz_idle";
    private static final String MODULE_NAME = "anatz";
    private static final UUID APP_GUID = UUID.randomUUID();
    private static final Map<String, String> APP_ENV = Map.of("LOG_LEVEL", "WARN");

    @Mock
    private DeploymentTypeDeterminer deploymentTypeDeterminer;
    @Mock
    private ProcessContext context;
    @Mock
    private StepLogger stepLogger;
    @Mock
    private CloudControllerClient client;
    private ApplicationEnvironmentCalculator applicationEnvironmentCalculator;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(context.getStepLogger()).thenReturn(stepLogger);
        when(context.getControllerClient()).thenReturn(client);
        applicationEnvironmentCalculator = new ApplicationEnvironmentCalculator(deploymentTypeDeterminer);
    }

    @Test
    void testShouldNotKeepExistingEnv() {
        var applicationToDeploy = getApplicationToDeploy();
        when(applicationToDeploy.getAttributesUpdateStrategy()).thenReturn(ImmutableCloudApplicationExtended.AttributeUpdateStrategy.builder()
                                                                                                                                    .shouldKeepExistingEnv(false)
                                                                                                                                    .build());
        Map<String, String> applicationEnv = applicationEnvironmentCalculator.calculateNewApplicationEnv(context, applicationToDeploy);
        assertEquals(APP_ENV, applicationEnv);
    }

    @Test
    void testShouldKeepExistingEnvWithDeployProcess() {
        when(deploymentTypeDeterminer.determineDeploymentType(context)).thenReturn(ProcessType.DEPLOY);
        var applicationToDeploy = getApplicationToDeploy();
        Map<String, String> applicationEnv = applicationEnvironmentCalculator.calculateNewApplicationEnv(context, applicationToDeploy);
        assertEquals(APP_ENV, applicationEnv);
    }

    @Test
    void testShouldKeepExistingEnvWithoutDeployedMta() {
        when(deploymentTypeDeterminer.determineDeploymentType(context)).thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        var applicationToDeploy = getApplicationToDeploy();
        Map<String, String> applicationEnv = applicationEnvironmentCalculator.calculateNewApplicationEnv(context, applicationToDeploy);
        assertEquals(APP_ENV, applicationEnv);
    }

    @Test
    void testShouldKeepExistingEnvWhenNewModuleIsAdded() {
        when(deploymentTypeDeterminer.determineDeploymentType(context)).thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        DeployedMta deployedMta = Mockito.mock(DeployedMta.class);
        DeployedMtaApplication deployedMtaApplication = Mockito.mock(DeployedMtaApplication.class);
        when(deployedMtaApplication.getModuleName()).thenReturn(MODULE_NAME + "-another-app");
        when(deployedMta.getApplications()).thenReturn(List.of(deployedMtaApplication));
        when(context.getVariable(Variables.DEPLOYED_MTA)).thenReturn(deployedMta);
        Module moduleToDeploy = Mockito.mock(Module.class);
        when(moduleToDeploy.getName()).thenReturn(MODULE_NAME + "-another-app");
        when(context.getVariable(Variables.MODULE_TO_DEPLOY)).thenReturn(moduleToDeploy);
        var applicationToDeploy = getApplicationToDeploy();
        Map<String, String> applicationEnv = applicationEnvironmentCalculator.calculateNewApplicationEnv(context, applicationToDeploy);
        assertEquals(APP_ENV, applicationEnv);
    }

    @Test
    void testShouldKeepExistingEnvWhenThereIsLiveAppDeployed() {
        when(deploymentTypeDeterminer.determineDeploymentType(context)).thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        DeployedMta deployedMta = Mockito.mock(DeployedMta.class);
        DeployedMtaApplication deployedMtaApplication = Mockito.mock(DeployedMtaApplication.class);
        when(deployedMtaApplication.getModuleName()).thenReturn(MODULE_NAME);
        when(deployedMtaApplication.getName()).thenReturn(APP_NAME);
        when(deployedMtaApplication.getProductizationState()).thenReturn(DeployedMtaApplication.ProductizationState.LIVE);
        CloudMetadata cloudMetadata = Mockito.mock(CloudMetadata.class);
        when(cloudMetadata.getGuid()).thenReturn(APP_GUID);
        when(deployedMtaApplication.getMetadata()).thenReturn(cloudMetadata);
        when(deployedMta.getApplications()).thenReturn(List.of(deployedMtaApplication));
        when(context.getVariable(Variables.DEPLOYED_MTA)).thenReturn(deployedMta);
        Module moduleToDeploy = Mockito.mock(Module.class);
        when(moduleToDeploy.getName()).thenReturn(MODULE_NAME);
        when(context.getVariable(Variables.MODULE_TO_DEPLOY)).thenReturn(moduleToDeploy);
        Map<String, String> existingAppEnv = Map.of("existing-key", "existing-value");
        when(client.getApplicationEnvironment(APP_GUID)).thenReturn(existingAppEnv);
        var applicationToDeploy = getApplicationToDeploy();
        Map<String, String> applicationEnv = applicationEnvironmentCalculator.calculateNewApplicationEnv(context, applicationToDeploy);
        assertEquals(MapUtil.merge(existingAppEnv, APP_ENV), applicationEnv);
    }

    private CloudApplicationExtended getApplicationToDeploy() {
        var applicationToDeploy = Mockito.mock(CloudApplicationExtended.class);
        when(applicationToDeploy.getEnv()).thenReturn(APP_ENV);
        when(applicationToDeploy.getName()).thenReturn(APP_NAME);
        when(applicationToDeploy.getModuleName()).thenReturn(MODULE_NAME);
        when(applicationToDeploy.getAttributesUpdateStrategy()).thenReturn(ImmutableCloudApplicationExtended.AttributeUpdateStrategy.builder()
                                                                                                                                    .shouldKeepExistingEnv(true)
                                                                                                                                    .build());
        return applicationToDeploy;
    }

}
