package org.cloudfoundry.multiapps.controller.core.validators.parameters.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ModuleParametersCompatibilityValidatorTest {

    @Mock
    private UserMessageLogger userMessageLogger;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    static Stream<Arguments> testModuleParametersCompatability() {
        return Stream.of(Arguments.of(List.of(SupportedParameters.HOST, SupportedParameters.ROUTES), true),
                         Arguments.of(List.of(SupportedParameters.ROUTES, SupportedParameters.IDLE_ROUTES), false),
                         Arguments.of(List.of(SupportedParameters.HOSTS, SupportedParameters.DOMAINS, SupportedParameters.ROUTES), true),
                         Arguments.of(List.of(SupportedParameters.IDLE_ROUTES, SupportedParameters.IDLE_HOST), true),
                         Arguments.of(List.of(SupportedParameters.IDLE_ROUTES, SupportedParameters.IDLE_HOSTS,
                                              SupportedParameters.IDLE_DOMAINS),
                                      true),
                         Arguments.of(List.of(SupportedParameters.ROUTES, SupportedParameters.IDLE_ROUTES, SupportedParameters.BUILDPACKS),
                                      false),
                         Arguments.of(List.of(SupportedParameters.ROUTES, SupportedParameters.IDLE_ROUTES, "not-supported-parameter"),
                                      false),
                         Arguments.of(List.of(SupportedParameters.HOSTS, SupportedParameters.ROUTES, SupportedParameters.ROUTE_PATH,
                                              SupportedParameters.IDLE_HOSTS, SupportedParameters.IDLE_ROUTES),
                                      true));
    }

    @ParameterizedTest
    @MethodSource
    void testModuleParametersCompatability(List<String> moduleParameters, boolean shouldWarnMessage) {
        Module module = buildModule(moduleParameters);

        Module validatedModule = new ModuleParametersCompatabilityValidator(module, userMessageLogger).validate();

        assertEquals(module.getParameters(), validatedModule.getParameters());
        verifyUserMessageLogger(shouldWarnMessage);
    }

    private Module buildModule(List<String> moduleParameters) {
        Map<String, Object> moduleParametersMap = moduleParameters.stream()
                                                                  .collect(Collectors.toMap(parameterName -> parameterName,
                                                                                            parameterValue -> ""));
        return Module.createV2()
                     .setParameters(moduleParametersMap);
    }

    private void verifyUserMessageLogger(boolean shouldWarnMessage) {
        if (shouldWarnMessage) {
            verify(userMessageLogger, atLeastOnce()).warn(anyString(), any(Object[].class));
            return;
        }
        verify(userMessageLogger, never()).warn(anyString(), any(Object[].class));
    }
}
