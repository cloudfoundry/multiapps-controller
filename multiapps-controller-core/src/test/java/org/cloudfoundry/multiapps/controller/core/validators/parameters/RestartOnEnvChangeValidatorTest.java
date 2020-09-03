package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RestartOnEnvChangeValidatorTest {

    private final RestartOnEnvChangeValidator validator = new RestartOnEnvChangeValidator();

    @Test
    void testGetParameterName() {
        assertEquals("restart-on-env-change", validator.getParameterName());
    }

    @Test
    void testGetContainerType() {
        assertTrue(validator.getContainerType()
                            .isAssignableFrom(Module.class));
    }

    @ParameterizedTest
    @MethodSource
    void testValidate(Map<String, Boolean> restartParameters, boolean isValid) {
        assertEquals(isValid, validator.isValid(restartParameters, null));
    }

    static Stream<Arguments> testValidate() {
        return Stream.of(
                         // (1) All conditions are true
                         Arguments.of(Map.ofEntries(Map.entry("vcap-application", true), Map.entry("vcap-services", true),
                                                    Map.entry("user-provided", true)),
                                      true),
                         // (2) All conditions are false
                         Arguments.of(Map.ofEntries(Map.entry("vcap-application", false), Map.entry("vcap-services", false),
                                                    Map.entry("user-provided", false)),
                                      true),
                         // (3) Test with error value for vcap-application
                         Arguments.of(Map.ofEntries(Map.entry("vcap-application", "foo.bar"), Map.entry("vcap-services", false),
                                                    Map.entry("user-provided", false)),
                                      false),
                         // (4) Test with error value for vcap-services
                         Arguments.of(Map.ofEntries(Map.entry("vcap-application", false), Map.entry("vcap-services", "foo.bar"),
                                                    Map.entry("user-provided", true)),
                                      false),
                         // (5) Test with error value for user-provided
                         Arguments.of(Map.ofEntries(Map.entry("vcap-application", false), Map.entry("vcap-services", true),
                                                    Map.entry("user-provided", "bar.xyz")),
                                      false),
                         // (6) Test with not map object
                         Arguments.of(null, false),
                         // (7)
                         Arguments.of(Map.ofEntries(Map.entry("vcap-application", false), Map.entry("vcap-services", true),
                                                    Map.entry("user-provided", false)),
                                      true),
                         // (8)
                         Arguments.of(Map.ofEntries(Map.entry("vcap-application", true), Map.entry("vcap-services", true),
                                                    Map.entry("user-provided", false)),
                                      true),
                         // (9) Not all parameters set
                         Arguments.of(Map.ofEntries(Map.entry("vcap-services", false), Map.entry("user-provided", true)), true),
                         // (10)
                         Arguments.of(Map.ofEntries(Map.entry("user-provided", true)), true),
                         // (11)
                         Arguments.of(Map.ofEntries(Map.entry("vcap-application", false)), true));
    }

}
