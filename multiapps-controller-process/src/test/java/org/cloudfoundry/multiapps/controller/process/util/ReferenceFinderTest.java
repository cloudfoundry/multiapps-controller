package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReferenceFinderTest {

    private static final String KEY = "key";
    private static final String VALUE = "value";

    private static final String MODULE_NAME = "moduleName";

    private static final String RESOURCE_NAME = "resourceName";

    private static final String HOOK_NAME = "hook";

    private static final String PROVIDED_DEPENDENCY = "providedDependency";

    private static final String REQUIRED_DEPENDENCY = "requiredDependency";

    private static final String DEPENDENCY_PARAMETER = "DependencyParameter";

    private static final String MODULE_PARAMETER = "ModuleParameter";
    private static final String HOOK_PARAMETER = "ModuleHookParameter";
    private static final String GLOBAL_PARAMETER = "GlobalParameter";

    private static final String RESOURCE_PARAMETER = "ResourceParameter";
    private static final String RESOURCE_PARAMETER_2 = "ResourceParameter2";

    private static final String RESOURCE_PARAMETER_3 = "ResourceParameter3";
    private static final String REFERENCED_PARAMETER = "ReferencedParameter";

    private static final String NOT_REFERENCED_PARAMETER = "NotReferencedParameter";

    private static ReferenceFinder finder;

    @BeforeEach
    void setUp() throws Exception {
        finder = new ReferenceFinder();
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testReferenceFinderNotReferenced() {
        Map<String, Object> globalParameters = Map.of(GLOBAL_PARAMETER, VALUE);
        Assertions.assertFalse(
            finder.isParameterReferenced(createDeploymentDescriptor(createModules(), createResources(), globalParameters),
                                         NOT_REFERENCED_PARAMETER));
    }

    @Test
    void testReferenceFinderReferencedInParameters() {
        List<Module> moduleList = List.of(Module.createV3()
                                                .setParameters(Map.of(MODULE_PARAMETER, "${" + REFERENCED_PARAMETER + "}")));
        Assertions.assertTrue(
            finder.isParameterReferenced(createDeploymentDescriptor(moduleList, Collections.emptyList(), Collections.emptyMap()),
                                         REFERENCED_PARAMETER));
    }

    @Test
    void testReferenceFinderReferencedInDependency() {
        List<RequiredDependency> requiredDependencies = List.of(RequiredDependency.createV3()
                                                                                  .setName(REQUIRED_DEPENDENCY)
                                                                                  .setParameters(Map.of(DEPENDENCY_PARAMETER,
                                                                                                        "${" + REFERENCED_PARAMETER + "}")));
        List<Module> moduleList = List.of(Module.createV3()
                                                .setRequiredDependencies(requiredDependencies));
        Assertions.assertTrue(
            finder.isParameterReferenced(createDeploymentDescriptor(moduleList, Collections.emptyList(), Collections.emptyMap()),
                                         REFERENCED_PARAMETER));
    }

    private static List<Module> createModules() {
        List<RequiredDependency> requiredDependencies = createRequiredDependencies();
        List<ProvidedDependency> providedDependencies = List.of(ProvidedDependency.createV3()
                                                                                  .setName(PROVIDED_DEPENDENCY)
                                                                                  .setParameters(Map.of(DEPENDENCY_PARAMETER, VALUE)));
        List<RequiredDependency> hookDependencies = List.of(RequiredDependency.createV3()
                                                                              .setName(REQUIRED_DEPENDENCY)
                                                                              .setParameters(Map.of(DEPENDENCY_PARAMETER, VALUE)));
        List<Hook> hooks = List.of(Hook.createV3()
                                       .setName(HOOK_NAME)
                                       .setParameters(Map.of(HOOK_PARAMETER, VALUE))
                                       .setRequiredDependencies(hookDependencies));
        return List.of(createModule(MODULE_NAME, Map.of(MODULE_PARAMETER, VALUE), requiredDependencies, providedDependencies, hooks));
    }

    private static List<RequiredDependency> createRequiredDependencies() {
        return List.of(RequiredDependency.createV3()
                                         .setName(REQUIRED_DEPENDENCY)
                                         .setParameters(Map.of(DEPENDENCY_PARAMETER, VALUE)));
    }

    private static List<Resource> createResources() {
        return List.of(createResource(RESOURCE_NAME, Map.of(RESOURCE_PARAMETER, VALUE),
                                      Map.of(RESOURCE_PARAMETER, List.of(VALUE), RESOURCE_PARAMETER_2, Map.of(KEY, VALUE),
                                             RESOURCE_PARAMETER_3, 1), createRequiredDependencies()));
    }

    private static Module createModule(String moduleName, Map<String, Object> parameters, List<RequiredDependency> requiredDependencies,
                                       List<ProvidedDependency> providedDependencies, List<Hook> hooks) {
        return Module.createV3()
                     .setName(moduleName)
                     .setParameters(parameters)
                     .setRequiredDependencies(requiredDependencies)
                     .setProvidedDependencies(providedDependencies)
                     .setHooks(hooks);
    }

    private static Resource createResource(String resourceName, Map<String, Object> parameters, Map<String, Object> properties,
                                           List<RequiredDependency> requiredDependencies) {
        return Resource.createV3()
                       .setName(resourceName)
                       .setParameters(parameters)
                       .setRequiredDependencies(requiredDependencies)
                       .setProperties(properties);
    }

    private static DeploymentDescriptor createDeploymentDescriptor(List<Module> modules, List<Resource> resources,
                                                                   Map<String, Object> globalParameters) {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        descriptor.setModules(modules);
        descriptor.setResources(resources);
        descriptor.setParameters(globalParameters);
        return descriptor;
    }
}
