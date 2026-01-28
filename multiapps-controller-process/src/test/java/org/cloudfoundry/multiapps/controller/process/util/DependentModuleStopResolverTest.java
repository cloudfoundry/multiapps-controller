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
import static org.mockito.ArgumentMatchers.anyInt;
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

    private final DependentModuleStopResolver dependentModuleStopResolver = new DependentModuleStopResolver();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    void setupContext(List<Module> modules) {
        setupContext(modules, true);
    }

    void setupContext(List<Module> modules, boolean dependencyAwareStopOrder) {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        descriptor.setParameters(Map.of(SupportedParameters.BG_DEPENDENCY_AWARE_STOP_ORDER, dependencyAwareStopOrder));
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
        Module module = v3("test-module", null);
        setupContext(List.of(module));
        List<Module> result = dependentModuleStopResolver.resolveDependentModulesToStop(context, module);
        assertTrue(result.isEmpty(), "Expected the list to be empty");
    }

    @Test
    void testDependentModuleStopResolverUnsupported() {
        Module module = Module.createV2()
                              .setName("test-module");
        setupContext(List.of(module));
        List<Module> result = dependentModuleStopResolver.resolveDependentModulesToStop(context, module);
        verify(logger, times(1)).warn(anyString(), anyString(), anyInt(), anyInt());
        assertTrue(result.isEmpty(), "Expected the list to be empty");
    }

    @Test
    void testDependentModuleStopResolverEmptyDeployedAfter() {
        Module module = Module.createV3()
                              .setName("test-module")
                              .setDeployedAfter(Collections.emptyList());
        setupContext(List.of(module));
        List<Module> result = dependentModuleStopResolver.resolveDependentModulesToStop(context, module);
        assertTrue(result.isEmpty(), "Expected the list to be empty");
    }

    @Test
    void resolveDependentModulesLinearChain() {
        Module a = v3("A", "");
        Module b = v3("B", "A");
        Module c = v3("C", "B");
        setupContext(List.of(a, b, c));
        List<Module> result = dependentModuleStopResolver.resolveDependentModulesToStop(context, a);

        assertEquals(List.of("C", "B"), result.stream()
                                              .map(Module::getName)
                                              .toList());
    }

    @Test
    void resolveDependentModuleDiamond() {
        Module a = v3("A", "");
        Module b = v3("B", "A");
        Module c = v3("C", "A");
        Module d = v3("D", "B", "C");

        setupContext(List.of(a, b, c, d));

        List<Module> result = dependentModuleStopResolver.resolveDependentModulesToStop(context, a);

        assertEquals(List.of("D", "B", "C"), result.stream()
                                                   .map(Module::getName)
                                                   .toList());
    }

    @Test
    void returnsEmptyWhenFeatureFlagDisabled() {
        Module a = v3("A", "");
        Module b = v3("B", "A");
        setupContext(List.of(a, b), false);
        assertTrue(dependentModuleStopResolver.resolveDependentModulesToStop(context, a)
                                              .isEmpty());
    }

    @Test
    void noDependentModulesReturnsEmpty() {
        Module a = v3("A", "");
        Module b = v3("B", "");

        setupContext(List.of(a, b));
        assertTrue(dependentModuleStopResolver.resolveDependentModulesToStop(context, a)
                                              .isEmpty());
    }

    @Test
    void diamondDependencyPostOrderAndNoDuplicates() {
        Module a = v3("A", "");
        Module b = v3("B", "A");
        Module c = v3("C", "A");
        Module d = v3("D", "B", "C");

        setupContext(List.of(a, b, c, d));
        List<String> result = names(dependentModuleStopResolver.resolveDependentModulesToStop(context, a));

        assertEquals(List.of("D", "B", "C"), result);
        assertEquals(result.size(), result.stream()
                                          .distinct()
                                          .count());
    }

    @Test
    void multipleBranchesAllIncluded() {
        Module a = v3("A", "");
        Module b = v3("B", "A");
        Module c = v3("C", "A");
        Module d = v3("D", "A");

        setupContext(List.of(a, b, c, d));
        List<String> result = names(
            dependentModuleStopResolver.resolveDependentModulesToStop(context, a)
        );

        assertTrue(result.containsAll(List.of("B", "C", "D")));
    }

    @Test
    void cyclicDependencyDoesNotInfiniteLoop() {
        Module a = v3("A", "C");
        Module b = v3("B", "A");
        Module c = v3("C", "B");

        setupContext(List.of(a, b, c));
        List<String> result = names(
            dependentModuleStopResolver.resolveDependentModulesToStop(context, a)
        );

        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of("B", "C")));
    }

    @Test
    void missingDependencyIsIgnoredSafely() {
        Module a = v3("A", "");
        Module b = v3("B", "A", "X");

        setupContext(List.of(a, b));
        assertEquals(List.of("B"),
                     names(dependentModuleStopResolver
                               .resolveDependentModulesToStop(context, a)));
    }

    @Test
    void rootIsNotIncludedInResult() {
        Module a = v3("A", "");
        Module b = v3("B", "A");
        setupContext(List.of(a, b));
        List<String> result = names(
            dependentModuleStopResolver.resolveDependentModulesToStop(context, a)
        );

        assertEquals(List.of("B"), result);
        assertFalse(result.contains("A"));
    }

    @Test
    void orderingIsDeterministic() {
        Module a = v3("A", "C");
        Module c = v3("C", "A");
        Module b = v3("B", "A");

        setupContext(List.of(a, c, b));
        List<String> result = names(
            dependentModuleStopResolver.resolveDependentModulesToStop(context, a)
        );

        assertEquals(List.of("B", "C"), result);
    }

    @Test
    void testDependentModuleStopResolverContextFlag() {
        Module a = v3("A", "");
        Module b = v3("B", "A");
        setupContext(List.of(a, b));
        Mockito.when(execution.getVariable(Variables.STOP_ORDER_IS_DEPENDENCY_AWARE.getName()))
               .thenReturn(true);
        List<String> result = names(
            dependentModuleStopResolver.resolveDependentModulesToStop(context, a)
        );

        assertEquals(List.of("B"), result);
        assertFalse(result.contains("A"));
    }

    @Test
    void testDependentModuleStopResolverSkip() {
        Module a = v3("A", "");
        Module b = v3("B", "A");
        setupContext(List.of(a, b));
        Mockito.when(context.getVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY))
               .thenReturn(false);

        List<String> result = names(
            dependentModuleStopResolver.resolveDependentModulesToStop(context, a)
        );
        verify(logger, times(1)).warn(anyString());
        assertTrue(result.isEmpty(), "Expected the list to be empty");

    }

    private static Module v3(String name, String... deployedAfter) {
        Module m = Module.createV3()
                         .setName(name);
        if (deployedAfter != null && deployedAfter.length > 0) {
            m.setDeployedAfter(List.of(deployedAfter));
        }
        return m;
    }

}

