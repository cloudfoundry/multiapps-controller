package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DependentModuleStopResolverTest {

    @Mock
    private ProcessContext context;

    @Mock
    private DelegateExecution execution;

    @Mock
    private StepLogger logger;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    void setupContext(List<Module> modules) {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        descriptor.setParameters(Map.of(SupportedParameters.BG_DEPENDENCY_AWARE_STOP_ORDER, true));
        Mockito.when(context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR))
               .thenReturn(descriptor);
        Mockito.when(context.getVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY))
               .thenReturn(true);
        descriptor.setModules(modules);
        Mockito.when(context.getExecution())
               .thenReturn(execution);
        Mockito.when(execution.getVariable(Variables.STOP_ORDER_IS_DEPENDENCY_AWARE.getName()))
               .thenReturn(false);
        Mockito.when(context.getStepLogger())
               .thenReturn(logger);
    }

    private List<String> names(List<Module> modules) {
        return modules.stream()
                      .map(Module::getName)
                      .toList();
    }

    @Test
    void testDependentModuleStopResolver() {
        Module module = Module.createV3()
                              .setName("test-module");
        setupContext(List.of(module));
        DependentModuleStopResolver dependentModuleStopResolver = new DependentModuleStopResolver();
        List<Module> result = dependentModuleStopResolver.resolveDependentModulesToStop(context, module);
        assertTrue(result.isEmpty(), "Expected the list to be empty");
    }

    @Test
    void testDependentModuleStopResolverUnsupported() {
        Module module = Module.createV2()
                              .setName("test-module");
        setupContext(List.of(module));
        DependentModuleStopResolver dependentModuleStopResolver = new DependentModuleStopResolver();
        List<Module> result = dependentModuleStopResolver.resolveDependentModulesToStop(context, module);
        assertTrue(result.isEmpty(), "Expected the list to be empty");
    }

    @Test
    void testDependentModuleStopResolverEmptyDeployedAfter() {
        Module module = Module.createV3()
                              .setName("test-module")
                              .setDeployedAfter(Collections.emptyList());
        setupContext(List.of(module));
        DependentModuleStopResolver dependentModuleStopResolver = new DependentModuleStopResolver();
        List<Module> result = dependentModuleStopResolver.resolveDependentModulesToStop(context, module);
        assertTrue(result.isEmpty(), "Expected the list to be empty");
    }

    @Test
    void resolveDependentModulesLinearChain() {
        Module a = Module.createV3()
                         .setName("A");
        Module b = Module.createV3()
                         .setName("B")
                         .setDeployedAfter(List.of("A"));
        Module c = Module.createV3()
                         .setName("C")
                         .setDeployedAfter(List.of("B"));

        setupContext(List.of(a, b, c));
        DependentModuleStopResolver resolver = new DependentModuleStopResolver();

        List<Module> result = resolver.resolveDependentModulesToStop(context, a);

        assertEquals(List.of("C", "B"), result.stream()
                                              .map(Module::getName)
                                              .toList());
    }

    @Test
    void resolveDependentModuleDiamond() {
        Module a = Module.createV3()
                         .setName("A");
        Module b = Module.createV3()
                         .setName("B")
                         .setDeployedAfter(List.of("A"));
        Module c = Module.createV3()
                         .setName("C")
                         .setDeployedAfter(List.of("A"));
        Module d = Module.createV3()
                         .setName("D")
                         .setDeployedAfter(List.of("B", "C"));

        setupContext(List.of(a, b, c, d));
        DependentModuleStopResolver resolver = new DependentModuleStopResolver();

        List<Module> result = resolver.resolveDependentModulesToStop(context, a);

        assertEquals(List.of("D", "B", "C"), result.stream()
                                                   .map(Module::getName)
                                                   .toList());
    }

    @Test
    void returnsEmptyWhenFeatureFlagDisabled() {
        Module a = Module.createV3()
                         .setName("A");
        Module b = Module.createV3()
                         .setName("B")
                         .setDeployedAfter(List.of("A"));
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        descriptor.setParameters(Map.of(SupportedParameters.BG_DEPENDENCY_AWARE_STOP_ORDER, false));
        Mockito.when(context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR))
               .thenReturn(descriptor);
        descriptor.setModules(List.of(a, b));
        Mockito.when(context.getExecution())
               .thenReturn(execution);
        Mockito.when(execution.getVariable(Variables.STOP_ORDER_IS_DEPENDENCY_AWARE.getName()))
               .thenReturn(false);
        DependentModuleStopResolver resolver = new DependentModuleStopResolver();
        assertTrue(resolver.resolveDependentModulesToStop(context, a)
                           .isEmpty());
    }

    @Test
    void noDependentModulesReturnsEmpty() {
        Module a = Module.createV3()
                         .setName("A");
        Module b = Module.createV3()
                         .setName("B");

        setupContext(List.of(a, b));
        DependentModuleStopResolver resolver = new DependentModuleStopResolver();

        assertTrue(resolver.resolveDependentModulesToStop(context, a)
                           .isEmpty());
    }

    @Test
    void diamondDependencyPostOrderAndNoDuplicates() {
        Module a = Module.createV3()
                         .setName("A");
        Module b = Module.createV3()
                         .setName("B")
                         .setDeployedAfter(List.of("A"));
        Module c = Module.createV3()
                         .setName("C")
                         .setDeployedAfter(List.of("A"));
        Module d = Module.createV3()
                         .setName("D")
                         .setDeployedAfter(List.of("B", "C"));
        setupContext(List.of(a, b, c, d));
        DependentModuleStopResolver resolver = new DependentModuleStopResolver();

        List<String> result = names(resolver.resolveDependentModulesToStop(context, a));

        assertEquals(List.of("D", "B", "C"), result);
        assertEquals(result.size(), result.stream()
                                          .distinct()
                                          .count());
    }

    @Test
    void multipleBranchesAllIncluded() {
        Module a = Module.createV3()
                         .setName("A");
        Module b = Module.createV3()
                         .setName("B")
                         .setDeployedAfter(List.of("A"));
        Module c = Module.createV3()
                         .setName("C")
                         .setDeployedAfter(List.of("A"));
        Module d = Module.createV3()
                         .setName("D")
                         .setDeployedAfter(List.of("A"));

        setupContext(List.of(a, b, c, d));
        List<String> result = names(
            new DependentModuleStopResolver().resolveDependentModulesToStop(context, a)
        );

        assertTrue(result.containsAll(List.of("B", "C", "D")));
    }

    @Test
    void cyclicDependencyDoesNotInfiniteLoop() {
        Module a = Module.createV3()
                         .setName("A")
                         .setDeployedAfter(List.of("C"));
        Module b = Module.createV3()
                         .setName("B")
                         .setDeployedAfter(List.of("A"));
        Module c = Module.createV3()
                         .setName("C")
                         .setDeployedAfter(List.of("B"));

        setupContext(List.of(a, b, c));
        List<String> result = names(
            new DependentModuleStopResolver().resolveDependentModulesToStop(context, a)
        );

        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of("B", "C")));
    }

    @Test
    void missingDependencyIsIgnoredSafely() {
        Module a = Module.createV3()
                         .setName("A");
        Module b = Module.createV3()
                         .setName("B")
                         .setDeployedAfter(List.of("A", "X"));

        setupContext(List.of(a, b));
        assertEquals(List.of("B"),
                     names(new DependentModuleStopResolver()
                               .resolveDependentModulesToStop(context, a)));
    }

    @Test
    void rootIsNotIncludedInResult() {
        Module a = Module.createV3()
                         .setName("A");
        Module b = Module.createV3()
                         .setName("B")
                         .setDeployedAfter(List.of("A"));
        setupContext(List.of(a, b));
        List<String> result = names(
            new DependentModuleStopResolver().resolveDependentModulesToStop(context, a)
        );

        assertEquals(List.of("B"), result);
        assertFalse(result.contains("A"));
    }

    @Test
    void orderingIsDeterministic() {
        Module a = Module.createV3()
                         .setName("A");
        Module c = Module.createV3()
                         .setName("C")
                         .setDeployedAfter(List.of("A"));
        Module b = Module.createV3()
                         .setName("B")
                         .setDeployedAfter(List.of("A"));
        setupContext(List.of(a, c, b));
        List<String> result = names(
            new DependentModuleStopResolver().resolveDependentModulesToStop(context, a)
        );

        assertEquals(List.of("B", "C"), result);
    }

    @Test
    void testDependentModuleStopResolverContextFlag() {
        Module a = Module.createV3()
                         .setName("A");
        Module b = Module.createV3()
                         .setName("B")
                         .setDeployedAfter(List.of("A"));

        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        descriptor.setParameters(Map.of(SupportedParameters.BG_DEPENDENCY_AWARE_STOP_ORDER, true));
        Mockito.when(context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR))
               .thenReturn(descriptor);
        descriptor.setModules(List.of(a, b));
        Mockito.when(context.getExecution())
               .thenReturn(execution);
        Mockito.when(context.getVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY))
               .thenReturn(true);
        Mockito.when(execution.getVariable(Variables.STOP_ORDER_IS_DEPENDENCY_AWARE.getName()))
               .thenReturn(true);

        List<String> result = names(
            new DependentModuleStopResolver().resolveDependentModulesToStop(context, a)
        );

        assertEquals(List.of("B"), result);
        assertFalse(result.contains("A"));
    }

    @Test
    void testDependentModuleStopResolverSkip() {
        Module a = Module.createV3()
                         .setName("A");
        Module b = Module.createV3()
                         .setName("B")
                         .setDeployedAfter(List.of("A"));
        setupContext(List.of(a, b));
        Mockito.when(context.getVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY))
               .thenReturn(false);

        List<String> result = names(
            new DependentModuleStopResolver().resolveDependentModulesToStop(context, a)
        );
        verify(logger, times(1)).warn(anyString());
        assertTrue(result.isEmpty(), "Expected the list to be empty");

    }
}

