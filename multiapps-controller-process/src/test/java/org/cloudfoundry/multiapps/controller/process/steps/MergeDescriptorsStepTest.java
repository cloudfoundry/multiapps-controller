package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaDescriptorMerger;
import org.cloudfoundry.multiapps.controller.core.test.DescriptorTestUtil;
import org.cloudfoundry.multiapps.controller.process.security.SecretParametersCollector;
import org.cloudfoundry.multiapps.controller.process.util.UnsupportedParameterFinder;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MergeDescriptorsStepTest extends SyncFlowableStepTest<MergeDescriptorsStep> {

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 2;

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = DescriptorTestUtil.loadDeploymentDescriptor("node-hello-mtad.yaml",
                                                                                                                  MergeDescriptorsStepTest.class);

    private class MergeDescriptorsStepMock extends MergeDescriptorsStep {

        @Override
        protected MtaDescriptorMerger getMtaDescriptorMerger(CloudHandlerFactory factory, Platform platform) {
            return merger;
        }

    }

    @Mock
    private MtaDescriptorMerger merger;

    @Mock
    private UnsupportedParameterFinder unsupportedParameterFinder;

    @Mock
    private SecretParametersCollector secretParametersCollector;

    @BeforeEach
    public void setUp() {
        prepareContext();
    }

    private void prepareContext() {
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);

        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, DEPLOYMENT_DESCRIPTOR);
        context.setVariable(Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN, Collections.emptyList());
    }

    @Test
    void testExecute1() {
        when(unsupportedParameterFinder.findUnsupportedParameters(Mockito.any(), Mockito.any())).thenReturn(Collections.emptyMap());
        when(merger.merge(any(), eq(Collections.emptyList()), eq(Collections.emptyList()))).thenReturn(DEPLOYMENT_DESCRIPTOR);

        step.execute(execution);
        verify(unsupportedParameterFinder, times(1)).findUnsupportedParameters(Mockito.any(), Mockito.any());
        assertStepFinishedSuccessfully();

        tester.test(() -> context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR),
                    new Expectation(Expectation.Type.JSON, "node-hello-mtad.yaml.json"));
    }

    @Test
    void testExecute2() {
        when(merger.merge(any(), eq(Collections.emptyList()), eq(Collections.emptyList()))).thenThrow(new ContentException("Error!"));
        assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Override
    protected MergeDescriptorsStep createStep() {
        return new MergeDescriptorsStepMock();
    }

}
