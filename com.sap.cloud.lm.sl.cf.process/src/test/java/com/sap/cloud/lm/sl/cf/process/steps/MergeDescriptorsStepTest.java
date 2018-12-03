package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadDeploymentDescriptor;
import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadPlatform;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorMerger;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v1.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1.Platform;

public class MergeDescriptorsStepTest extends SyncFlowableStepTest<MergeDescriptorsStep> {

    private static final ConfigurationParser CONFIGURATION_PARSER = new ConfigurationParser();

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 1;

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = loadDeploymentDescriptor("node-hello-mtad.yaml",
        MergeDescriptorsStepTest.class);

    private static final Platform PLATFORM = loadPlatform(CONFIGURATION_PARSER, "platform-01.json", MergeDescriptorsStepTest.class);

    private class MergeDescriptorsStepMock extends MergeDescriptorsStep {

        @Override
        protected MtaDescriptorMerger getMtaDescriptorMerger(HandlerFactory factory, Platform platform) {
            return merger;
        }

    }

    @Mock
    private MtaDescriptorMerger merger;

    @Before
    public void setUp() throws Exception {
        prepareContext();
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);

        StepsUtil.setUnresolvedDeploymentDescriptor(context, DEPLOYMENT_DESCRIPTOR);
        StepsUtil.setExtensionDescriptorChain(context, Collections.emptyList());

        StepsUtil.setAsBinaryJson(context, Constants.VAR_PLATFORM, PLATFORM);
    }

    @Test
    public void testExecute1() throws Exception {
        when(merger.merge(any(), eq(Collections.emptyList()))).thenReturn(DEPLOYMENT_DESCRIPTOR);

        step.execute(context);

        assertStepFinishedSuccessfully();

        TestUtil.test(() -> {

            return StepsUtil.getUnresolvedDeploymentDescriptor(context);

        }, new Expectation(Expectation.Type.RESOURCE, "node-hello-mtad.yaml.json"), getClass());
    }

    @Test(expected = SLException.class)
    public void testExecute2() throws Exception {
        when(merger.merge(any(), eq(Collections.emptyList()))).thenThrow(new ContentException("Error!"));

        step.execute(context);
    }

    @Override
    protected MergeDescriptorsStep createStep() {
        return new MergeDescriptorsStepMock();
    }

}
