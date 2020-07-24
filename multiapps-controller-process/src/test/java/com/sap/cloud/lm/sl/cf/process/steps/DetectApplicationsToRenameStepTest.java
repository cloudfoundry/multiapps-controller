package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.ImmutableMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.BlueGreenApplicationNameSuffix;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class DetectApplicationsToRenameStepTest extends SyncFlowableStepTest<DetectApplicationsToRenameStep> {

    @BeforeEach
    public void setUp() {
        context.setVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY, true);
    }

    @Test
    public void testNoExecuteWithoutParam() {
        context.setVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY, false);
        step.execute(execution);
        assertStepFinishedSuccessfully();
    }

    @Test
    public void testExecuteWithoutDeployedMta() {
        step.execute(execution);
        assertStepFinishedSuccessfully();
    }

    public static Stream<Arguments> testExecuteWithoutRenamingApps() {
        return Stream.of(
// @formatter:off
            Arguments.of("a-live", "b-live"),
            Arguments.of("a-idle", "b-idle"),
            Arguments.of("a-live", "b-idle")
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testExecuteWithoutRenamingApps(String appName1, String appName2) {
        DeployedMta deployedMta = createDeployedMta(appName1, appName2);
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);

        step.execute(execution);
        assertStepFinishedSuccessfully();

        Assertions.assertTrue(context.getVariable(Variables.APPS_TO_RENAME)
                                     .isEmpty());
        Assertions.assertTrue(context.getVariable(Variables.APPS_TO_UNDEPLOY)
                                     .isEmpty());
    }

    @Test
    public void testExecuteFailsOnException() {
        Mockito.when(execution.getVariable(Mockito.anyString()))
               .thenThrow(new SLException("exception"));
        Assertions.assertThrows(SLException.class, () -> step.execute(execution), "exception");
    }

    @Test
    public void testExecuteRenamesApps() {
        List<String> appsToUpdate = Arrays.asList("a", "b");

        DeployedMta deployedMta = createDeployedMta(appsToUpdate.toArray(new String[0]));
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);

        step.execute(execution);
        assertStepFinishedSuccessfully();

        Assertions.assertEquals(appsToUpdate, context.getVariable(Variables.APPS_TO_RENAME));

        List<String> appsToCheck = appsToUpdate.stream()
                                               .map(name -> name + BlueGreenApplicationNameSuffix.LIVE.asSuffix())
                                               .collect(Collectors.toList());
        validateUpdatedDeployedMta(appsToCheck);
    }

    @Test
    public void testExecuteWithTwoVersionsOfAppDeletesOldAndRenamesNew() {
        DeployedMta deployedMta = createDeployedMta("a-live", "a");
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);

        CloudControllerClient client = Mockito.mock(CloudControllerClient.class);
        Mockito.when(client.getApplication("a-live", false))
               .thenReturn(createApplication("a-live"));
        Mockito.when(context.getControllerClient())
               .thenReturn(client);

        step.execute(execution);
        assertStepFinishedSuccessfully();

        Assertions.assertTrue(context.getVariable(Variables.APPS_TO_UNDEPLOY)
                                     .contains(createApplication("a-live")));
        Assertions.assertTrue(context.getVariable(Variables.APPS_TO_RENAME)
                                     .contains("a"));

        List<String> appsToCheck = Collections.singletonList("a-live");
        validateUpdatedDeployedMta(appsToCheck);
    }

    @Test
    public void testExecuteWithTwoVersionsOfAppRenamesOld() {
        DeployedMta deployedMta = createDeployedMta("a-idle", "a");
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);

        step.execute(execution);
        assertStepFinishedSuccessfully();

        Assertions.assertTrue(context.getVariable(Variables.APPS_TO_RENAME)
                                     .contains("a"));
        Assertions.assertTrue(context.getVariable(Variables.APPS_TO_UNDEPLOY)
                                     .isEmpty());

        List<String> appsToCheck = Arrays.asList("a-idle", "a-live");
        validateUpdatedDeployedMta(appsToCheck);
    }

    @Test
    public void testExecuteWithTwoVersionsOfAppDoesNothing() {
        DeployedMta deployedMta = createDeployedMta("a-live", "a-idle");
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);

        step.execute(execution);
        assertStepFinishedSuccessfully();

        Assertions.assertTrue(context.getVariable(Variables.APPS_TO_RENAME)
                                     .isEmpty());
        Assertions.assertTrue(context.getVariable(Variables.APPS_TO_UNDEPLOY)
                                     .isEmpty());

        List<String> appsToCheck = Arrays.asList("a-idle", "a-live");
        validateUpdatedDeployedMta(appsToCheck);
    }

    private CloudApplication createApplication(String name) {
        return ImmutableCloudApplication.builder()
                                        .name(name)
                                        .build();
    }

    private DeployedMta createDeployedMta(String... appNames) {
        List<DeployedMtaApplication> deployedApps = Arrays.stream(appNames)
                                                          .map(this::createDeployedApp)
                                                          .collect(Collectors.toList());
        return ImmutableDeployedMta.builder()
                                   .metadata(ImmutableMtaMetadata.builder()
                                                                 .id("")
                                                                 .build())
                                   .applications(deployedApps)
                                   .build();
    }

    private DeployedMtaApplication createDeployedApp(String appName) {
        return ImmutableDeployedMtaApplication.builder()
                                              .moduleName("")
                                              .name(appName)
                                              .build();
    }

    private void validateUpdatedDeployedMta(List<String> appsToCheck) {
        DeployedMta updatedDeployedMta = context.getVariable(Variables.DEPLOYED_MTA);
        Assertions.assertTrue(updatedDeployedMta.getApplications()
                                                .stream()
                                                .allMatch(module -> appsToCheck.contains(module.getName())));
    }

    @Override
    protected DetectApplicationsToRenameStep createStep() {
        return new DetectApplicationsToRenameStep();
    }

}
