package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HookPhasesConfigValidatorTest {

    private final HookPhasesConfigValidator validator = new HookPhasesConfigValidator();

    @Test
    void testValidatePassesWhenDescriptorHasNoModules() {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        descriptor.setModules(List.of());

        Assertions.assertDoesNotThrow(() -> validator.validate(descriptor));
    }

    @Test
    void testValidatePassesWhenModuleHasNoHooks() {
        DeploymentDescriptor descriptor = descriptorWithModule(moduleWithHooks(List.of()));

        Assertions.assertDoesNotThrow(() -> validator.validate(descriptor));
    }

    @Test
    void testValidatePassesWhenHookHasNoPhasesConfigParameter() {
        Hook hook = hook("my-hook", List.of("deploy.application.before-start"), Map.of());
        DeploymentDescriptor descriptor = descriptorWithModule(moduleWithHooks(List.of(hook)));

        Assertions.assertDoesNotThrow(() -> validator.validate(descriptor));
    }

    @Test
    void testValidatePassesWhenPhasesConfigHasUniquePhases() {
        List<Map<String, String>> phasesConfig = List.of(Map.of("phase", "deploy.application.before-start", "target-app", "live"),
                                                         Map.of("phase", "deploy.application.after-start", "target-app", "idle"));
        Hook hook = hook("my-hook", List.of("deploy.application.before-start"),
                         Map.of(SupportedParameters.PHASES_CONFIG, phasesConfig));
        DeploymentDescriptor descriptor = descriptorWithModule(moduleWithHooks(List.of(hook)));

        Assertions.assertDoesNotThrow(() -> validator.validate(descriptor));
    }

    @Test
    void testValidateThrowsWhenPhasesConfigIsNotAList() {
        Hook hook = hook("bad-hook", List.of("deploy.application.before-start"),
                         Map.of(SupportedParameters.PHASES_CONFIG, "not-a-list"));
        DeploymentDescriptor descriptor = descriptorWithModule(moduleWithHooks(List.of(hook)));

        SLException exception = Assertions.assertThrows(SLException.class, () -> validator.validate(descriptor));
        Assertions.assertTrue(exception.getMessage()
                                       .contains("bad-hook"));
        Assertions.assertTrue(exception.getMessage()
                                       .contains("phases-config"));
    }

    @Test
    void testValidateThrowsWhenPhasesConfigContainsDuplicatePhase() {
        List<Map<String, String>> phasesConfig = List.of(Map.of("phase", "deploy.application.before-start", "target-app", "live"),
                                                         Map.of("phase", "deploy.application.before-start", "target-app", "idle"));
        Hook hook = hook("dup-hook", List.of("deploy.application.before-start"),
                         Map.of(SupportedParameters.PHASES_CONFIG, phasesConfig));
        DeploymentDescriptor descriptor = descriptorWithModule(moduleWithHooks(List.of(hook)));

        SLException exception = Assertions.assertThrows(SLException.class, () -> validator.validate(descriptor));
        Assertions.assertTrue(exception.getMessage()
                                       .contains("deploy.application.before-start"));
        Assertions.assertTrue(exception.getMessage()
                                       .contains("dup-hook"));
    }

    @Test
    void testValidateIgnoresEntriesWithoutPhaseKey() {
        List<Map<String, String>> phasesConfig = List.of(Map.of("target-app", "live"), Map.of("target-app", "idle"));
        Hook hook = hook("no-phase-key", List.of("deploy.application.before-start"),
                         Map.of(SupportedParameters.PHASES_CONFIG, phasesConfig));
        DeploymentDescriptor descriptor = descriptorWithModule(moduleWithHooks(List.of(hook)));

        Assertions.assertDoesNotThrow(() -> validator.validate(descriptor));
    }

    @Test
    void testValidatePassesAndDoesNotThrowWhenDeprecatedPhaseUsed() {
        Hook hook = hook("deprecated-hook", List.of(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_UNMAP_ROUTES_IDLE.getValue()),
                         Map.of());
        DeploymentDescriptor descriptor = descriptorWithModule(moduleWithHooks(List.of(hook)));

        Assertions.assertDoesNotThrow(() -> validator.validate(descriptor));
    }

    private DeploymentDescriptor descriptorWithModule(Module module) {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        descriptor.setModules(List.of(module));
        return descriptor;
    }

    private Module moduleWithHooks(List<Hook> hooks) {
        Module module = Module.createV3()
                              .setName("module-1")
                              .setType("application");
        module.setHooks(hooks);
        return module;
    }

    private Hook hook(String name, List<String> phases, Map<String, Object> parameters) {
        return Hook.createV3()
                   .setName(name)
                   .setPhases(phases)
                   .setParameters(parameters);
    }

}