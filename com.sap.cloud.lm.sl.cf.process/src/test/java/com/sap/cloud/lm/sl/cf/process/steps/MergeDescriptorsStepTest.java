package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorMerger;
import com.sap.cloud.lm.sl.cf.core.util.DescriptorTestUtil;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Platform;

public class MergeDescriptorsStepTest extends SyncFlowableStepTest<MergeDescriptorsStep> {

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 2;

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = DescriptorTestUtil.loadDeploymentDescriptor("node-hello-mtad.yaml",
                                                                                                                  MergeDescriptorsStepTest.class);

    private class MergeDescriptorsStepMock extends MergeDescriptorsStep {

        @Override
        protected MtaDescriptorMerger getMtaDescriptorMerger(HandlerFactory factory, Platform platform) {
            return merger;
        }

    }

    @Mock
    private MtaDescriptorMerger merger;

    @Before
    public void setUp() {
        prepareContext();
    }

    private void prepareContext() {
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);

        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, DEPLOYMENT_DESCRIPTOR);
        StepsUtil.setExtensionDescriptorChain(execution, Collections.emptyList());
    }

    @Test
    public void testExecute1() {
        when(merger.merge(any(), eq(Collections.emptyList()))).thenReturn(DEPLOYMENT_DESCRIPTOR);

        step.execute(execution);

        assertStepFinishedSuccessfully();

        tester.test(() -> context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR),
                    new Expectation(Expectation.Type.JSON, "node-hello-mtad.yaml.json"));
    }

    @Test(expected = SLException.class)
    public void testExecute2() {
        when(merger.merge(any(), eq(Collections.emptyList()))).thenThrow(new ContentException("Error!"));

        step.execute(execution);
    }

    @Override
    protected MergeDescriptorsStep createStep() {
        return new MergeDescriptorsStepMock();
    }

}
