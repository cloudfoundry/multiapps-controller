package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorPropertiesResolver;
import com.sap.cloud.lm.sl.cf.core.util.DescriptorTestUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

public class ProcessDescriptorStepTest extends SyncFlowableStepTest<ProcessDescriptorStep> {

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 2;

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = DescriptorTestUtil.loadDeploymentDescriptor("node-hello-mtad.yaml",
                                                                                                                  ProcessDescriptorStepTest.class);

    private class ProcessDescriptorStepMock extends ProcessDescriptorStep {

        @Override
        protected MtaDescriptorPropertiesResolver getMtaDescriptorPropertiesResolver(DelegateExecution context) {
            return resolver;
        }

    }

    @Mock
    private MtaDescriptorPropertiesResolver resolver;

    @Before
    public void setUp() throws Exception {
        prepareContext();
    }

    private void prepareContext() throws Exception {
        StepsUtil.setDeploymentDescriptorWithSystemParameters(context, DEPLOYMENT_DESCRIPTOR);

        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SERVICE_ID, Constants.DEPLOY_SERVICE_ID);
        context.setVariable(Constants.PARAM_USE_NAMESPACES, false);
        context.setVariable(Constants.PARAM_USE_NAMESPACES_FOR_SERVICES, false);

        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);
    }

    @Test
    public void testExecute1() throws Exception {
        when(resolver.resolve(any())).thenAnswer((invocation) -> invocation.getArguments()[0]);

        step.execute(context);

        assertStepFinishedSuccessfully();

        tester.test(() -> StepsUtil.getSubscriptionsToCreate(context), new Expectation("[]"));

        tester.test(() -> StepsUtil.getCompleteDeploymentDescriptor(context),
                    new Expectation(Expectation.Type.JSON, "node-hello-mtad-1.yaml.json"));
    }

    @Test(expected = SLException.class)
    public void testExecute2() throws Exception {
        when(resolver.resolve(any())).thenThrow(new SLException("Error!"));

        step.execute(context);
    }

    @Test(expected = SLException.class)
    public void testWithInvalidModulesSpecifiedForDeployment() {
        when(resolver.resolve(any())).thenReturn(DEPLOYMENT_DESCRIPTOR);
        when(context.getVariable(Constants.PARAM_MODULES_FOR_DEPLOYMENT)).thenReturn("foo,bar");

        step.execute(context);
    }

    @Override
    protected ProcessDescriptorStep createStep() {
        return new ProcessDescriptorStepMock();
    }

}
