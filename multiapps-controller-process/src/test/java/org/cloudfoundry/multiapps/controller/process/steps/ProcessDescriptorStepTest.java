package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaDescriptorPropertiesResolver;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.test.DescriptorTestUtil;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessDescriptorStepTest extends SyncFlowableStepTest<ProcessDescriptorStep> {

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 2;

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = DescriptorTestUtil.loadDeploymentDescriptor("node-hello-mtad.yaml",
                                                                                                                  ProcessDescriptorStepTest.class);

    private class ProcessDescriptorStepMock extends ProcessDescriptorStep {

        public ProcessDescriptorStepMock(ModuleToDeployHelper moduleToDeployHelper) {
            super(null, moduleToDeployHelper);
        }

        @Override
        protected MtaDescriptorPropertiesResolver getMtaDescriptorPropertiesResolver(ProcessContext context,
                                                                                     DeploymentDescriptor descriptor) {
            return resolver;
        }

    }

    @Mock
    private MtaDescriptorPropertiesResolver resolver;
    @Mock
    private ModuleToDeployHelper moduleToDeployHelper;

    @BeforeEach
    public void setUp() throws Exception {
        prepareContext();
    }

    private void prepareContext() {
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS, DEPLOYMENT_DESCRIPTOR);

        context.setVariable(Variables.SERVICE_ID, Constants.DEPLOY_SERVICE_ID);
        context.setVariable(Variables.MTA_NAMESPACE, null);

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
        context.setVariable(Variables.MODULES_FOR_DEPLOYMENT, List.of("foo", "bar"));
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

    static Stream<Arguments> testSetMtaModulesBasedOnSkipDeployParameter() {
        return Stream.of(Arguments.of(true, false, Set.of("test-module-2")), Arguments.of(false, true, Set.of("test-module-1")),
                         Arguments.of(false, false, Set.of("test-module-1", "test-module-2")),
                         Arguments.of(true, true, Collections.emptySet()));
    }

    @ParameterizedTest
    @MethodSource
    void testSetMtaModulesBasedOnSkipDeployParameter(boolean skipDeploy1, boolean skipDeploy2, Set<String> expectedModuleNames) {
        prepareContextForSkipDeployParameters(skipDeploy1, skipDeploy2);

        step.execute(execution);

        Set<String> mtaModules = context.getVariable(Variables.MTA_MODULES);
        assertEquals(expectedModuleNames, mtaModules);
    }

    private void prepareContextForSkipDeployParameters(boolean skipDeploy1, boolean skipDeploy2) {
        Module module1 = Module.createV3()
                               .setName("test-module-1");
        module1.setParameters(Map.of(SupportedParameters.SKIP_DEPLOY, skipDeploy1));

        Module module2 = Module.createV3()
                               .setName("test-module-2");
        module2.setParameters(Map.of(SupportedParameters.SKIP_DEPLOY, skipDeploy2));

        DeploymentDescriptor descriptor = DEPLOYMENT_DESCRIPTOR;
        descriptor.setModules(List.of(module1, module2));

        when(resolver.resolve(any())).thenReturn(descriptor);
        when(moduleToDeployHelper.shouldSkipDeploy(module1)).thenReturn(skipDeploy1);
        when(moduleToDeployHelper.shouldSkipDeploy(module2)).thenReturn(skipDeploy2);

        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS, descriptor);
    }

    @Override
    protected ProcessDescriptorStep createStep() {
        return new ProcessDescriptorStepMock(moduleToDeployHelper);
    }

}