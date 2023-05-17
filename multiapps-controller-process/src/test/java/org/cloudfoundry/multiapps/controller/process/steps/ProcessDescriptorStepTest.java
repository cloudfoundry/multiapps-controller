package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaDescriptorPropertiesResolver;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.test.DescriptorTestUtil;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class ProcessDescriptorStepTest extends SyncFlowableStepTest<ProcessDescriptorStep> {

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 2;

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = DescriptorTestUtil.loadDeploymentDescriptor("node-hello-mtad.yaml",
                                                                                                                  ProcessDescriptorStepTest.class);

    private class ProcessDescriptorStepMock extends ProcessDescriptorStep {

        @Override
        protected MtaDescriptorPropertiesResolver getMtaDescriptorPropertiesResolver(ProcessContext context) {
            return resolver;
        }

    }

    @Mock
    private MtaDescriptorPropertiesResolver resolver;

    @BeforeEach
    public void setUp() throws Exception {
        prepareContext();
    }

    private void prepareContext() {
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS, DEPLOYMENT_DESCRIPTOR);

        context.setVariable(Variables.SERVICE_ID, Constants.DEPLOY_SERVICE_ID);
        context.setVariable(Variables.MTA_NAMESPACE, null);
        context.setVariable(Variables.APPLY_NAMESPACE, false);

        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);
    }

    @Test
    void testExecute1() {
        when(resolver.resolve(any())).thenAnswer((invocation) -> invocation.getArguments()[0]);

        step.execute(execution);

        assertStepFinishedSuccessfully();

        tester.test(() -> context.getVariable(Variables.SUBSCRIPTIONS_TO_CREATE), new Expectation("[]"));

        tester.test(() -> context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR),
                    new Expectation(Expectation.Type.JSON, "node-hello-mtad-1.yaml.json"));
    }

    @Test
    void testExecute2() {
        when(resolver.resolve(any())).thenThrow(new SLException("Error!"));
        assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Test
    void testWithInvalidModulesSpecifiedForDeployment() {
        when(resolver.resolve(any())).thenReturn(DEPLOYMENT_DESCRIPTOR);
        when(context.getVariable(Variables.MODULES_FOR_DEPLOYMENT)).thenReturn(List.of("foo", "bar"));
        assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Test
    void testDynamicParametersAreSetOnlyOnce() {
        when(resolver.resolve(any())).thenAnswer((invocation) -> invocation.getArguments()[0]);        
        Set<DynamicResolvableParameter> dynamicParameters = Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                   .parameterName("service-guid")
                                                                   .relationshipEntityName("service-1")
                                                                   .value("service-guid")
                                                                   .build());
        context.setVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETERS, dynamicParameters);
        
        step.execute(execution);

        verify(execution, times(1)).setVariable(eq(Variables.DYNAMIC_RESOLVABLE_PARAMETERS.getName()), any());
    }

    @Override
    protected ProcessDescriptorStep createStep() {
        return new ProcessDescriptorStepMock();
    }

}
