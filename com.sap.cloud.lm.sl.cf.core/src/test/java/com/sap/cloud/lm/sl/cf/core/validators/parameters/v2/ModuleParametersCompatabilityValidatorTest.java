package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.mta.model.Module;

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
            Arguments.of(Arrays.asList(SupportedParameters.HOST, SupportedParameters.ROUTES), true, Arrays.asList(SupportedParameters.HOST)),
            Arguments.of(Arrays.asList(SupportedParameters.ROUTES, SupportedParameters.IDLE_ROUTES), false, Collections.emptyList()),
            Arguments.of(Arrays.asList(SupportedParameters.HOSTS, SupportedParameters.DOMAINS, SupportedParameters.ROUTES), true, Arrays.asList(SupportedParameters.HOSTS, SupportedParameters.DOMAINS)),
            Arguments.of(Arrays.asList(SupportedParameters.IDLE_ROUTES, SupportedParameters.IDLE_HOST), true, Arrays.asList(SupportedParameters.IDLE_HOST)),
            Arguments.of(Arrays.asList(SupportedParameters.IDLE_ROUTES, SupportedParameters.IDLE_HOSTS, SupportedParameters.IDLE_DOMAINS), true, 
                         Arrays.asList(SupportedParameters.IDLE_HOSTS, SupportedParameters.IDLE_DOMAINS))
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testModuleParametersCompatability(List<String> moduleParameters, boolean shouldWarnMessage,
                                                  List<String> expectedMissingModuleParameters) {
        Module module = buildModule(moduleParameters);

        Module validatedModule = new ModuleParametersCompatabilityValidator(module, userMessageLogger).validate();

        assertMissingModuleParameters(expectedMissingModuleParameters, validatedModule.getParameters());
        verifyUserMessageLogger(shouldWarnMessage);
    }

    private void assertMissingModuleParameters(List<String> expectedMissingModuleParameters, Map<String, Object> parameters) {
        if (expectedMissingModuleParameters.stream()
                                           .anyMatch(expectedMissingParameter -> parameters.containsKey(expectedMissingParameter))) {
            fail(MessageFormat.format("Expected to miss the following parameters: {0} but the result contains {1}",
                                      expectedMissingModuleParameters, parameters.keySet()));
        }
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
            verify(userMessageLogger).warn(anyString(), any());
            return;
        }
        verify(userMessageLogger, never()).warn(anyString(), any());
    }
}
