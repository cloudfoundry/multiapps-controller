package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.ApplicationColor;
import org.cloudfoundry.multiapps.controller.core.model.BlueGreenApplicationNameSuffix;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.test.DescriptorTestUtil;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationColorDetector;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class RenameApplicationsStepTest extends SyncFlowableStepTest<RenameApplicationsStep> {

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 2;

    @Mock
    private ApplicationColorDetector applicationColorDetector;

    @BeforeEach
    void setUp() {
        prepareContext();
        context.setVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY, false);
    }

    private void prepareContext() {
        context.setVariable(Variables.DEPLOYED_MTA,
                            JsonUtil.fromJson(TestUtil.getResourceAsString("deployed-mta-01.json", getClass()), DeployedMta.class));

        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);

        DeploymentDescriptor descriptor = DescriptorTestUtil.loadDeploymentDescriptor("node-hello-mtad.yaml", getClass());
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);
    }

    @Test
    void testOldNewSuffixRenaming() {
        context.setVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY, true);
        context.setVariable(Variables.APPS_TO_RENAME, List.of("a"));

        step.execute(execution);
        assertStepFinishedSuccessfully();

        Mockito.verify(client)
               .rename("a", "a-live");

        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        Assertions.assertTrue(descriptor.getModules()
                                        .stream()
                                        .map(NameUtil::getApplicationName)
                                        .allMatch(name -> name.endsWith(BlueGreenApplicationNameSuffix.IDLE.asSuffix())));
    }

    @Test
    void testWithNoColorsDeployed() {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenReturn(null);

        step.execute(execution);

        assertStepFinishedSuccessfully();

        tester.test(() -> context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR),
                    new Expectation(Expectation.Type.JSON, "node-hello-blue-mtad.yaml.json"));
    }

    @Test
    void testWithOneColorDeployed() {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any(DeployedMta.class))).thenReturn(ApplicationColor.GREEN);

        step.execute(execution);

        assertStepFinishedSuccessfully();

        tester.test(() -> context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR),
                    new Expectation(Expectation.Type.JSON, "node-hello-blue-mtad.yaml.json"));
    }

    @Test
    void testWithTwoColorsDeployed() {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenThrow(new ConflictException(Messages.CONFLICTING_APP_COLORS));
        when(applicationColorDetector.detectLiveApplicationColor(any(), any())).thenReturn(ApplicationColor.GREEN);
        step.execute(execution);

        assertStepFinishedSuccessfully();

        tester.test(() -> context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR),
                    new Expectation(Expectation.Type.JSON, "node-hello-blue-mtad.yaml.json"));
    }

    @Test
    void testExceptionIsThrown() {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenThrow(new SLException(org.cloudfoundry.multiapps.controller.process.Messages.ERROR_RENAMING_APPLICATIONS));
        when(applicationColorDetector.detectLiveApplicationColor(any(), any())).thenReturn(ApplicationColor.GREEN);
        Assertions.assertThrows(SLException.class, () -> step.execute(execution),
                                org.cloudfoundry.multiapps.controller.process.Messages.ERROR_RENAMING_APPLICATIONS);
    }

    @Override
    protected RenameApplicationsStep createStep() {
        return new RenameApplicationsStep();
    }

}
