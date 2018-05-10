package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.DomainsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class ResolveConfigurationEntriesTest extends BuildCloudDeployModelStepTest {

    private class ResolveConfigurationEntriesStepMock extends ResolveConfigurationEntriesStep {
        @Override
        protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DelegateExecution context) {
            return applicationsCloudModelBuilder;
        }

        @Override
        protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DelegateExecution context) {
            return servicesCloudModelBuilder;
        }

        @Override
        protected DomainsCloudModelBuilder getDomainsCloudModelBuilder(DelegateExecution context) {
            return domainsCloudModelBuilder;
        }

        @Override
        protected ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DelegateExecution context,
            DeploymentDescriptor deploymentDescriptor) {
            return serviceKeysCloudModelBuilder;
        }
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                new StepInput("apps-to-deploy-05.json", "services-to-bind-01.json", "services-to-create-01.json", "service-keys-01.json", "configurations-to-publish-01.json", Arrays.asList("custom-domain-1", "custom-domain-2"), "deployed-mta-12.json", ProcessType.DEPLOY), new StepOutput("0.1.0"),
            },
            {
                new StepInput("apps-to-deploy-05.json", "services-to-bind-01.json", "services-to-create-01.json", "service-keys-01.json", "configurations-to-publish-01.json", Arrays.asList("custom-domain-1", "custom-domain-2"), null, ProcessType.DEPLOY), new StepOutput("0.1.0"),
            },
            {
                new StepInput("apps-to-deploy-05.json", "services-to-bind-01.json", "services-to-create-01.json", "service-keys-01.json", "configurations-to-publish-01.json", Arrays.asList("custom-domain-1", "custom-domain-2"), null, ProcessType.BLUE_GREEN_DEPLOY), new StepOutput("0.1.0"),
            },
// @formatter:on
        });
    }

    public ResolveConfigurationEntriesTest(StepInput input, StepOutput output) {
        super(input, output);
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        assertConfigurationEntriesToPublish();
    }

    private void assertConfigurationEntriesToPublish() {
        assertFalse(StepsUtil.getSkipUpdateConfigurationEntries(context));
        TestUtil.test(() -> StepsUtil.getConfigurationEntriesToPublish(context), "R:" + input.configurationsToPublishLocation, getClass());
    }

    @Override
    protected BuildCloudDeployModelStep createStep() {
        return new ResolveConfigurationEntriesStepMock();
    }
}
