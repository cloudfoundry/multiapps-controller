package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sap.cloud.lm.sl.cf.core.model.BlueGreenApplicationNameSuffix;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.util.DescriptorTestUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationColorDetector;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import org.mockito.Mockito;

import java.util.Collections;

public class RenameApplicationsStepTest extends SyncFlowableStepTest<RenameApplicationsStep> {

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 2;

    @Mock
    private ApplicationColorDetector applicationColorDetector;

    @BeforeEach
    public void setUp() {
        prepareContext();
        context.setVariable(Constants.PARAM_KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY, false);
    }

    private void prepareContext() {
        execution.setVariable(Variables.DEPLOYED_MTA,
                              JsonUtil.fromJson(TestUtil.getResourceAsString("deployed-mta-01.json", getClass()), DeployedMta.class));

        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);

        DeploymentDescriptor descriptor = DescriptorTestUtil.loadDeploymentDescriptor("node-hello-mtad.yaml", getClass());
        execution.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);
    }

    @Test
    public void testOldNewSuffixRenaming() {
        context.setVariable(Constants.PARAM_KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY, true);
        execution.setVariable(Variables.APPS_TO_RENAME, Collections.singletonList("a"));

        step.execute(context);
        assertStepFinishedSuccessfully();

        Mockito.verify(client)
               .rename("a", "a-live");

        DeploymentDescriptor descriptor = execution.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        Assertions.assertTrue(descriptor.getModules()
                                        .stream()
                                        .map(NameUtil::getApplicationName)
                                        .allMatch(name -> name.endsWith(BlueGreenApplicationNameSuffix.IDLE.asSuffix())));
    }

    @Test
    public void testWithNoColorsDeployed() {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenReturn(null);

        step.execute(context);

        assertStepFinishedSuccessfully();

        tester.test(() -> execution.getVariable(Variables.DEPLOYMENT_DESCRIPTOR),
                    new Expectation(Expectation.Type.JSON, "node-hello-blue-mtad.yaml.json"));
    }

    @Test
    public void testWithOneColorDeployed() {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any(DeployedMta.class))).thenReturn(ApplicationColor.GREEN);

        step.execute(context);

        assertStepFinishedSuccessfully();

        tester.test(() -> execution.getVariable(Variables.DEPLOYMENT_DESCRIPTOR),
                    new Expectation(Expectation.Type.JSON, "node-hello-blue-mtad.yaml.json"));
    }

    @Test
    public void testWithTwoColorsDeployed() {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenThrow(new ConflictException(Messages.CONFLICTING_APP_COLORS));
        when(applicationColorDetector.detectLiveApplicationColor(any(), any())).thenReturn(ApplicationColor.GREEN);
        step.execute(context);

        assertStepFinishedSuccessfully();

        tester.test(() -> execution.getVariable(Variables.DEPLOYMENT_DESCRIPTOR),
                    new Expectation(Expectation.Type.JSON, "node-hello-blue-mtad.yaml.json"));
    }

    @Test
    public void testExceptionIsThrown() {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenThrow(new SLException(com.sap.cloud.lm.sl.cf.process.Messages.ERROR_RENAMING_APPLICATIONS));
        when(applicationColorDetector.detectLiveApplicationColor(any(), any())).thenReturn(ApplicationColor.GREEN);
        Assertions.assertThrows(SLException.class, () -> step.execute(context),
                                com.sap.cloud.lm.sl.cf.process.Messages.ERROR_RENAMING_APPLICATIONS);
    }

    @Override
    protected RenameApplicationsStep createStep() {
        return new RenameApplicationsStep();
    }

}
