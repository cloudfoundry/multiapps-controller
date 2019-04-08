package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadDeploymentDescriptor;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.helpers.ApplicationColorDetector;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;

public class BlueGreenRenameStepTest extends SyncFlowableStepTest<BlueGreenRenameStep> {

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 2;

    @Mock
    private ApplicationColorDetector applicationColorDetector;

    @Before
    public void setUp() throws Exception {
        prepareContext();
        step.applicationColorDetector = mock(ApplicationColorDetector.class);
    }

    private void prepareContext() throws Exception {
        StepsUtil.setDeployedMta(context,
            JsonUtil.fromJson(TestUtil.getResourceAsString("deployed-mta-01.json", getClass()), DeployedMta.class));

        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);

        StepsUtil.setDeploymentDescriptor(context, loadDeploymentDescriptor("node-hello-mtad.yaml", getClass()));
    }

    // Test what happens when there are 0 color(s) deployed:
    @Test
    public void testExecute0() throws Exception {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenReturn(null);

        step.execute(context);

        assertStepFinishedSuccessfully();

        TestUtil.test(() -> StepsUtil.getDeploymentDescriptor(context),
            new Expectation(Expectation.Type.RESOURCE, "node-hello-blue-mtad.yaml.json"), getClass());
    }

    // Test what happens when there are 1 color(s) deployed:
    @Test
    public void testExecute1() throws Exception {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any())).thenReturn(ApplicationColor.GREEN);

        step.execute(context);

        assertStepFinishedSuccessfully();

        TestUtil.test(() -> StepsUtil.getDeploymentDescriptor(context),
            new Expectation(Expectation.Type.RESOURCE, "node-hello-blue-mtad.yaml.json"), getClass());
    }

    // Test what happens when there are 2 color(s) deployed:
    @Test
    public void testExecute2() throws Exception {
        when(applicationColorDetector.detectSingularDeployedApplicationColor(any()))
            .thenThrow(new ConflictException(Messages.CONFLICTING_APP_COLORS));
        when(applicationColorDetector.detectLiveApplicationColor(any(), any())).thenReturn(ApplicationColor.GREEN);
        step.execute(context);

        assertStepFinishedSuccessfully();

        TestUtil.test(() -> StepsUtil.getDeploymentDescriptor(context),
            new Expectation(Expectation.Type.RESOURCE, "node-hello-blue-mtad.yaml.json"), getClass());
    }

    @Override
    protected BlueGreenRenameStep createStep() {
        return new BlueGreenRenameStep();
    }

}
