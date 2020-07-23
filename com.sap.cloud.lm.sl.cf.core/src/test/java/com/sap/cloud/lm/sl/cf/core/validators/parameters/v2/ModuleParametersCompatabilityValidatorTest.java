package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;

public class ModuleParametersCompatabilityValidatorTest {

    @Mock
    private UserMessageLogger userMessageLogger;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    public static Stream<Arguments> testModuleParametersCompatability() {
        return Stream.of(
        // @formatter:off
            Arguments.of(Arrays.asList(SupportedParameters.HOST, SupportedParameters.ROUTES), true),
            Arguments.of(Arrays.asList(SupportedParameters.ROUTES, SupportedParameters.IDLE_ROUTES), false),
            Arguments.of(Arrays.asList(SupportedParameters.HOSTS, SupportedParameters.DOMAINS, SupportedParameters.ROUTES), true),
            Arguments.of(Arrays.asList(SupportedParameters.IDLE_ROUTES, SupportedParameters.IDLE_HOST), true),
            Arguments.of(Arrays.asList(SupportedParameters.IDLE_ROUTES, SupportedParameters.IDLE_HOSTS, SupportedParameters.IDLE_DOMAINS), true),
            Arguments.of(Arrays.asList(SupportedParameters.ROUTES, SupportedParameters.IDLE_ROUTES, SupportedParameters.BUILDPACKS), false),
            Arguments.of(Arrays.asList(SupportedParameters.ROUTES, SupportedParameters.IDLE_ROUTES, "not-supported-parameter"), false),
            Arguments.of(Arrays.asList(SupportedParameters.HOSTS, SupportedParameters.ROUTES, SupportedParameters.ROUTE_PATH, SupportedParameters.IDLE_HOSTS, SupportedParameters.IDLE_ROUTES), true)
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testModuleParametersCompatability(List<String> moduleParameters, boolean shouldWarnMessage) {
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
            verify(userMessageLogger, atLeastOnce()).warn(anyString(), any());
            return;
        }
        verify(userMessageLogger, never()).warn(anyString(), any());
    }
}
