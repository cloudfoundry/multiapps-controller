package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class SupportedParametersCheckerTest {

    private static final String VALUE = "value";

    private static final String MODULE_NAME = "moduleName";

    private static final String RESOURCE_NAME = "resourceName";

    private static final String HOOK_NAME = "hook";

    private static final String PROVIDED_DEPENDENCY = "providedDependency";

    private static final String REQUIRED_DEPENDENCY = "requiredDependency";

    private static final String SUPPORTED_DEPENDENCY_PARAMETER = "SupportedDependencyParameter";

    private static final String UNSUPPORTED_DEPENDENCY_PARAMETER = "UnsupportedDependencyParameter";
    private static final String SUPPORTED_MODULE_PARAMETER = "SupportedModuleParameter";
    private static final String UNSUPPORTED_MODULE_PARAMETER = "UnsupportedModuleParameter";
    private static final String SUPPORTED_HOOK_PARAMETER = "SupportedModuleHookParameter";
    private static final String UNSUPPORTED_HOOK_PARAMETER = "UnsupportedModuleHookParameter";
    private static final String SUPPORTED_GLOBAL_PARAMETER = "SupportedGlobalParameter";
    private static final String UNSUPPORTED_GLOBAL_PARAMETER = "UnsupportedGlobalParameter";

    private static final String SUPPORTED_RESOURCE_PARAMETER = "SupportedResourceParameter";
    private static final String UNSUPPORTED_RESOURCE_PARAMETER = "UnsupportedResourceParameter";

    private static final String UNSUPPORTED_HOOK_DEPENDENCY_PARAMETER = "UnsupportedHookDependencyParameter";

    private static final String UNSUPPORTED_PROVIDED_DEPENDENCY_PARAMETER = "UnsupportedProvidedDependencyParameter";
    @InjectMocks
    private SupportedParametersChecker checker;

    @Mock
    private ReferenceFinder finder;

    @BeforeEach
    void setUp() throws Exception {
        checker = new CustomSupportedParametersChecker();
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(finder.isParameterReferenced(Mockito.any(), Mockito.anyString()))
               .thenReturn(false);
    }

    @Test
    void testCheckSupportedParametersForModule() {
        List<Module> moduleList = createModules();
        List<Resource> resourceList = Collections.emptyList();
        List<String> unsupportedParameters = checker.getUnknownParameters(createDeploymentDescriptor(moduleList, resourceList,
                                                                                                     Collections.emptyMap()));
        Assertions.assertEquals(List.of(UNSUPPORTED_MODULE_PARAMETER, UNSUPPORTED_HOOK_PARAMETER, UNSUPPORTED_HOOK_DEPENDENCY_PARAMETER,
                                        UNSUPPORTED_DEPENDENCY_PARAMETER, UNSUPPORTED_PROVIDED_DEPENDENCY_PARAMETER),
                                unsupportedParameters);
    }

    @Test
    void testCheckSupportedParametersForResources() {
        List<Resource> resourceList = createResources();
        List<Module> moduleList = Collections.emptyList();
        List<String> unsupportedParameters = checker.getUnknownParameters(createDeploymentDescriptor(moduleList, resourceList,
                                                                                                     Collections.emptyMap()));
        Assertions.assertEquals(List.of(UNSUPPORTED_RESOURCE_PARAMETER, UNSUPPORTED_DEPENDENCY_PARAMETER), unsupportedParameters);
    }

    @Test
    void testCheckSupportedGlobalParameters() {
        List<Module> moduleList = Collections.emptyList();
        List<Resource> resourceList = Collections.emptyList();
        Map<String, Object> globalParameters = Map.of(SUPPORTED_GLOBAL_PARAMETER, VALUE, UNSUPPORTED_GLOBAL_PARAMETER, VALUE);
        List<String> unsupportedParameters = checker.getUnknownParameters(createDeploymentDescriptor(moduleList, resourceList,
                                                                                                     globalParameters));
        Assertions.assertEquals(List.of(UNSUPPORTED_GLOBAL_PARAMETER), unsupportedParameters);
    }

    @Test
    void testUnsupportedParametersForAllEntities() {
        List<Module> moduleList = createModules();
        List<Resource> resourceList = createResources();
        Map<String, Object> globalParameters = Map.of(SUPPORTED_GLOBAL_PARAMETER, VALUE, UNSUPPORTED_GLOBAL_PARAMETER, VALUE);
        List<String> unsupportedParameters = checker.getUnknownParameters(createDeploymentDescriptor(moduleList, resourceList,
                                                                                                     globalParameters));
        Assertions.assertEquals(List.of(UNSUPPORTED_MODULE_PARAMETER, UNSUPPORTED_HOOK_PARAMETER, UNSUPPORTED_HOOK_DEPENDENCY_PARAMETER,
                                        UNSUPPORTED_DEPENDENCY_PARAMETER, UNSUPPORTED_PROVIDED_DEPENDENCY_PARAMETER,
                                        UNSUPPORTED_RESOURCE_PARAMETER, UNSUPPORTED_DEPENDENCY_PARAMETER, UNSUPPORTED_GLOBAL_PARAMETER),
                                unsupportedParameters);
    }

    private static List<Module> createModules() {
        List<RequiredDependency> requiredDependencies = createRequiredDependencies();
        List<ProvidedDependency> providedDependencies = List.of(ProvidedDependency.createV3()
                                                                                  .setName(PROVIDED_DEPENDENCY)
                                                                                  .setParameters(Map.of(SUPPORTED_DEPENDENCY_PARAMETER,
                                                                                                        VALUE,
                                                                                                        UNSUPPORTED_PROVIDED_DEPENDENCY_PARAMETER,
                                                                                                        VALUE)));
        List<RequiredDependency> hookDependencies = List.of(RequiredDependency.createV3()
                                                                              .setName(REQUIRED_DEPENDENCY)
                                                                              .setParameters(Map.of(SUPPORTED_DEPENDENCY_PARAMETER, VALUE,
                                                                                                    UNSUPPORTED_HOOK_DEPENDENCY_PARAMETER,
                                                                                                    VALUE)));
        List<Hook> hooks = List.of(Hook.createV3()
                                       .setName(HOOK_NAME)
                                       .setParameters(Map.of(SUPPORTED_HOOK_PARAMETER, VALUE, UNSUPPORTED_HOOK_PARAMETER, VALUE))
                                       .setRequiredDependencies(hookDependencies));
        return List.of(createModule(MODULE_NAME, Map.of(SUPPORTED_MODULE_PARAMETER, VALUE, UNSUPPORTED_MODULE_PARAMETER, VALUE),
                                    requiredDependencies, providedDependencies, hooks));
    }

    private static List<RequiredDependency> createRequiredDependencies() {
        return List.of(RequiredDependency.createV3()
                                         .setName(REQUIRED_DEPENDENCY)
                                         .setParameters(Map.of(SUPPORTED_DEPENDENCY_PARAMETER, VALUE, UNSUPPORTED_DEPENDENCY_PARAMETER,
                                                               VALUE)));
    }

    private static List<Resource> createResources() {
        return List.of(createResource(RESOURCE_NAME, Map.of(SUPPORTED_RESOURCE_PARAMETER, VALUE, UNSUPPORTED_RESOURCE_PARAMETER, VALUE),
                                      createRequiredDependencies()));
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

    private static Resource createResource(String resourceName, Map<String, Object> parameters,
                                           List<RequiredDependency> requiredDependencies) {
        return Resource.createV3()
                       .setName(resourceName)
                       .setParameters(parameters)
                       .setRequiredDependencies(requiredDependencies);
    }

    private static DeploymentDescriptor createDeploymentDescriptor(List<Module> modules, List<Resource> resources,
                                                                   Map<String, Object> globalParameters) {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        descriptor.setModules(modules);
        descriptor.setResources(resources);
        descriptor.setParameters(globalParameters);
        return descriptor;
    }

    private static class CustomSupportedParametersChecker extends SupportedParametersChecker {
        @Override
        protected Set<String> getDependencyParameters() {
            return Set.of(SUPPORTED_DEPENDENCY_PARAMETER);
        }

        @Override
        protected Set<String> getModuleParameters() {
            return Set.of(SUPPORTED_MODULE_PARAMETER);
        }

        @Override
        protected Set<String> getModuleHookParameters() {
            return Set.of(SUPPORTED_HOOK_PARAMETER);
        }

        @Override
        protected Set<String> getResourceParameters() {
            return Set.of(SUPPORTED_RESOURCE_PARAMETER);
        }

        @Override
        protected Set<String> getGlobalParameters() {
            return Set.of(SUPPORTED_GLOBAL_PARAMETER);
        }

    }
}
