package org.cloudfoundry.multiapps.controller.core.validators.parameters.v2;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DescriptorParametersCompatabilityValidatorTest {

    private static final String MODULE_NAME = "test-module";
    private static final Map<String, Object> MODULE_PARAMETERS = Map.of("test-param", "test-value");

    @Mock
    private UserMessageLogger userMessageLogger;
    @Mock
    private ModuleParametersCompatabilityValidator moduleParametersCompatabilityValidator;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testDescriptorValidator() {
        Module module = buildModule();
        DeploymentDescriptor descriptor = buildDeploymentDescriptor(module);

        prepareModuleParametersCompatabilityValidator(module);

        DescriptorParametersCompatabilityValidator descriptorValidator = new DescriptorParametersCompatabilityValidator(descriptor,
                                                                                                                        userMessageLogger) {
            @Override
            protected ModuleParametersCompatabilityValidator getModuleParametersCompatabilityValidator(Module module) {
                return moduleParametersCompatabilityValidator;
            }

        };

        DeploymentDescriptor validatedDescriptor = descriptorValidator.validate();
        assertModules(module, validatedDescriptor.getModules());
    }

    private Module buildModule() {
        return Module.createV2()
                     .setName(MODULE_NAME)
                     .setParameters(MODULE_PARAMETERS);
    }

    private DeploymentDescriptor buildDeploymentDescriptor(Module module) {
        return DeploymentDescriptor.createV2()
                                   .setModules(Collections.singletonList(module));
    }

    private void prepareModuleParametersCompatabilityValidator(Module module) {
        when(moduleParametersCompatabilityValidator.validate()).thenReturn(module);
    }

    private void assertModules(Module expectedModule, List<Module> validatedModules) {
        assertTrue(validatedModules.contains(expectedModule));
        assertTrue(validatedModules.stream()
                                   .map(Module::getParameters)
                                   .map(Map::keySet)
                                   .flatMap(Collection::stream)
                                   .anyMatch(parameterKey -> isContainsParamerKey(expectedModule, parameterKey)));
    }

    private boolean isContainsParamerKey(Module expectedModule, String parameterKey) {
        return expectedModule.getParameters()
                             .containsKey(parameterKey);
    }

}
