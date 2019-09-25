package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.util.DescriptorTestUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.helpers.ApplicationColorDetector;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;

public class BlueGreenRenameStepTest extends SyncFlowableStepTest<BlueGreenRenameStep> {

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 2;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private ApplicationColorDetector applicationColorDetector;

    @Before
    public void setUp() throws Exception {
        prepareContext();
        step.applicationColorDetector = applicationColorDetector;
    }

    private void prepareContext() throws Exception {
        StepsUtil.setDeployedMta(context,
                                 JsonUtil.fromJson(TestUtil.getResourceAsString("deployed-mta-01.json", getClass()), DeployedMta.class));

        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);

        StepsUtil.setDeploymentDescriptor(context, DescriptorTestUtil.loadDeploymentDescriptor("node-hello-mtad.yaml", getClass()));
    }

    @Test
    public void testWithNoColorsDeployed() throws Exception {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenReturn(null);

        step.execute(context);

        assertStepFinishedSuccessfully();

        tester.test(() -> StepsUtil.getDeploymentDescriptor(context),
                    new Expectation(Expectation.Type.JSON, "node-hello-blue-mtad.yaml.json"));
    }

    @Test
    public void testWithOneColorDeployed() throws Exception {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any(DeployedMta.class))).thenReturn(ApplicationColor.GREEN);

        step.execute(context);

        assertStepFinishedSuccessfully();

        tester.test(() -> StepsUtil.getDeploymentDescriptor(context),
                    new Expectation(Expectation.Type.JSON, "node-hello-blue-mtad.yaml.json"));
    }

    @Test
    public void testWithTwoColorsDeployed() throws Exception {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenThrow(new ConflictException(Messages.CONFLICTING_APP_COLORS));
        when(applicationColorDetector.detectLiveApplicationColor(any(), any())).thenReturn(ApplicationColor.GREEN);
        step.execute(context);

        assertStepFinishedSuccessfully();

        tester.test(() -> StepsUtil.getDeploymentDescriptor(context),
                    new Expectation(Expectation.Type.JSON, "node-hello-blue-mtad.yaml.json"));
    }

    @Test
    public void testExceptionIsThrow() {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenThrow(new SLException(com.sap.cloud.lm.sl.cf.process.message.Messages.ERROR_RENAMING_MODULES));
        when(applicationColorDetector.detectLiveApplicationColor(any(), any())).thenReturn(ApplicationColor.GREEN);
        expectedException.expect(SLException.class);
        expectedException.expectMessage(com.sap.cloud.lm.sl.cf.process.message.Messages.ERROR_RENAMING_MODULES);
        step.execute(context);
        verify(context, never()).setVariable(anyString(), any());
    }

    @Override
    protected BlueGreenRenameStep createStep() {
        return new BlueGreenRenameStep();
    }

}
