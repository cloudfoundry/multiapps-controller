package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.ImmutableMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.BlueGreenApplicationNameSuffix;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DetectApplicationsToRenameStepTest extends SyncFlowableStepTest<DetectApplicationsToRenameStep> {

    @BeforeEach
    public void setUp() {
        context.setVariable(Constants.PARAM_KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY, true);
    }

    @Test
    public void testNoExecuteWithoutParam() {
        context.setVariable(Constants.PARAM_KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY, false);
        step.execute(context);
        assertStepFinishedSuccessfully();
    }
    
    @Test
    public void testExecuteWithoutDeployedMta() {
        step.execute(context);
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
        StepsUtil.setDeployedMta(context, deployedMta);

        step.execute(context);
        assertStepFinishedSuccessfully();

        Assertions.assertTrue(StepsUtil.getAppsToRename(context)
                                       .isEmpty());
        Assertions.assertTrue(StepsUtil.getAppsToUndeploy(context)
                                       .isEmpty());
    }

    @Test
    public void testExecuteFailsOnException() {
        Mockito.when(context.getVariable(Mockito.anyString()))
               .thenThrow(new SLException("exception"));
        Assertions.assertThrows(SLException.class, () -> step.execute(context), "exception");
    }

    @Test
    public void testExecuteRenamesApps() {
        List<String> appsToUpdate = Arrays.asList("a", "b");

        DeployedMta deployedMta = createDeployedMta(appsToUpdate.toArray(new String[0]));
        StepsUtil.setDeployedMta(context, deployedMta);

        step.execute(context);
        assertStepFinishedSuccessfully();

        Assertions.assertEquals(appsToUpdate, StepsUtil.getAppsToRename(context));

        List<String> appsToCheck = appsToUpdate.stream()
                                               .map(name -> name + BlueGreenApplicationNameSuffix.LIVE.asSuffix())
                                               .collect(Collectors.toList());
        validateUpdatedDeployedMta(appsToCheck);
    }

    @Test
    public void testExecuteWithTwoVersionsOfAppDeletesOldAndRenamesNew() {
        DeployedMta deployedMta = createDeployedMta("a-live", "a");
        StepsUtil.setDeployedMta(context, deployedMta);

        CloudControllerClient client = Mockito.mock(CloudControllerClient.class);
        Mockito.when(client.getApplication("a-live", false))
               .thenReturn(createApplication("a-live"));
        Mockito.when(execution.getControllerClient())
               .thenReturn(client);

        step.execute(context);
        assertStepFinishedSuccessfully();

        Assertions.assertTrue(StepsUtil.getAppsToUndeploy(context)
                                       .contains(createApplication("a-live")));
        Assertions.assertTrue(StepsUtil.getAppsToRename(context)
                                       .contains("a"));

        List<String> appsToCheck = Collections.singletonList("a-live");
        validateUpdatedDeployedMta(appsToCheck);
    }

    @Test
    public void testExecuteWithTwoVersionsOfAppRenamesOld() {
        DeployedMta deployedMta = createDeployedMta("a-idle", "a");
        StepsUtil.setDeployedMta(context, deployedMta);

        step.execute(context);
        assertStepFinishedSuccessfully();

        Assertions.assertTrue(StepsUtil.getAppsToRename(context)
                                       .contains("a"));
        Assertions.assertTrue(StepsUtil.getAppsToUndeploy(context)
                                       .isEmpty());

        List<String> appsToCheck = Arrays.asList("a-idle", "a-live");
        validateUpdatedDeployedMta(appsToCheck);
    }

    @Test
    public void testExecuteWithTwoVersionsOfAppDoesNothing() {
        DeployedMta deployedMta = createDeployedMta("a-live", "a-idle");
        StepsUtil.setDeployedMta(context, deployedMta);

        step.execute(context);
        assertStepFinishedSuccessfully();

        Assertions.assertTrue(StepsUtil.getAppsToRename(context)
                                       .isEmpty());
        Assertions.assertTrue(StepsUtil.getAppsToUndeploy(context)
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
        DeployedMta updatedDeployedMta = StepsUtil.getDeployedMta(context);
        Assertions.assertTrue(updatedDeployedMta.getApplications()
                                                .stream()
                                                .allMatch(module -> appsToCheck.contains(module.getName())));
    }

    @Override
    protected DetectApplicationsToRenameStep createStep() {
        return new DetectApplicationsToRenameStep();
    }

}
