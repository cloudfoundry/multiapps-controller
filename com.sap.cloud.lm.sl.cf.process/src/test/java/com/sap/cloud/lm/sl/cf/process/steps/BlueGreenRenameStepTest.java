package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadDeploymentDescriptor;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationColorDetector;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser;

public class BlueGreenRenameStepTest extends SyncActivitiStepTest<BlueGreenRenameStep> {

    private static final DescriptorParser DESCRIPTOR_PARSER = new DescriptorParser();

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 1;
    private static final Integer MTA_MINOR_SCHEMA_VERSION = 1;

    @Mock
    private ApplicationColorDetector applicationColorDetector;

    @Before
    public void setUp() throws Exception {
        prepareContext();
        step.colorDetectorSupplier = () -> applicationColorDetector;
    }

    private void prepareContext() throws Exception {
        StepsUtil.setDeployedMta(context,
            JsonUtil.fromJson(TestUtil.getResourceAsString("deployed-mta-01.json", getClass()), DeployedMta.class));

        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);
        context.setVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION, MTA_MINOR_SCHEMA_VERSION);

        StepsUtil.setUnresolvedDeploymentDescriptor(context,
            loadDeploymentDescriptor(DESCRIPTOR_PARSER, "node-hello-mtad.yaml", getClass()));
    }

    // Test what happens when there are 0 color(s) deployed:
    @Test
    public void testExecute0() throws Exception {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenReturn(null);

        step.execute(context);

        assertStepFinishedSuccessfully();

        TestUtil.test(() -> StepsUtil.getUnresolvedDeploymentDescriptor(context), "R:node-hello-blue-mtad.yaml.json", getClass());
    }

    // Test what happens when there are 1 color(s) deployed:
    @Test
    public void testExecute1() throws Exception {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenReturn(ApplicationColor.GREEN);

        step.execute(context);

        assertStepFinishedSuccessfully();

        TestUtil.test(() -> StepsUtil.getUnresolvedDeploymentDescriptor(context), "R:node-hello-blue-mtad.yaml.json", getClass());
    }

    // Test what happens when there are 2 color(s) deployed:
    @Test
    public void testExecute2() throws Exception {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenThrow(
            new ConflictException(Messages.CONFLICTING_APP_COLORS));
        when(applicationColorDetector.detectFirstDeployedApplicationColor(any())).thenReturn(ApplicationColor.GREEN);

        step.execute(context);

        assertStepFinishedSuccessfully();

        TestUtil.test(() -> StepsUtil.getUnresolvedDeploymentDescriptor(context), "R:node-hello-blue-mtad.yaml.json", getClass());
    }

    @Override
    protected BlueGreenRenameStep createStep() {
        return new BlueGreenRenameStep();
    }

}
