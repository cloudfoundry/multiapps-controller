package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadDeploymentDescriptor;
import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadPlatforms;
import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadTargets;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorMerger;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public class MergeDescriptorsStepTest extends SyncActivitiStepTest<MergeDescriptorsStep> {

    private static final ConfigurationParser CONFIGURATION_PARSER = new ConfigurationParser();
    private static final DescriptorParser DESCRIPTOR_PARSER = new DescriptorParser();

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 1;
    private static final Integer MTA_MINOR_SCHEMA_VERSION = 0;

    private static final Platform PLATFORM = loadPlatforms(CONFIGURATION_PARSER, "platform-types-01.json",
        MergeDescriptorsStepTest.class).get(0);
    private static final Target TARGET = loadTargets(CONFIGURATION_PARSER, "platforms-01.json", MergeDescriptorsStepTest.class).get(0);

    private class MergeDescriptorsStepMock extends MergeDescriptorsStep {

        @Override
        protected MtaDescriptorMerger getMtaDescriptorMerger(HandlerFactory factory, Platform platform, Target target) {
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
        context.setVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION, MTA_MINOR_SCHEMA_VERSION);

        StepsUtil.setExtensionDescriptorStrings(context, Collections.emptyList());
        StepsUtil.setDeploymentDescriptorString(context, "");

        ContextUtil.setAsBinaryJson(context, Constants.VAR_PLATFORM, PLATFORM);
        ContextUtil.setAsBinaryJson(context, Constants.VAR_TARGET, TARGET);
    }

    @Test
    public void testExecute1() throws Exception {
        when(merger.merge("", Collections.emptyList())).thenReturn(
            loadDeploymentDescriptor(DESCRIPTOR_PARSER, "node-hello-mtad.yaml", getClass()));

        step.execute(context);

        assertStepFinishedSuccessfully();

        TestUtil.test(() -> {

            return StepsUtil.getUnresolvedDeploymentDescriptor(context);

        }, "R:node-hello-mtad.yaml.json", getClass());
    }

    @Test(expected = SLException.class)
    public void testExecute2() throws Exception {
        when(merger.merge("", Collections.emptyList())).thenThrow(new ContentException("Error!"));

        step.execute(context);
    }

    @Override
    protected MergeDescriptorsStep createStep() {
        return new MergeDescriptorsStepMock();
    }

}
