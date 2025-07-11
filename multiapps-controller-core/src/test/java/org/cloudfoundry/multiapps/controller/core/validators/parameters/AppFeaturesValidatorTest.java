package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppFeaturesValidatorTest {

    private AppFeaturesValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AppFeaturesValidator();
    }

    private static Stream<Arguments> testIsValid() {
        return Stream.of(
            // (1) All features are boolean
            Arguments.of(Map.of("featureA", true, "featureB", false), true),
            // (2) Some features are not boolean
            Arguments.of(Map.of("featureA", true, "featureB", "notBoolean"), false),
            // (3) Empty map is valid
            Arguments.of(Map.of(), true),
            // (4) null object is invalid
            Arguments.of(null, false),
            // (5) Non-map object is invalid
            Arguments.of("notAMap", false));
    }

    @ParameterizedTest
    @MethodSource
    void testIsValid(Object parameters, boolean expected) {
        assertEquals(expected, validator.isValid(parameters, null));
    }

    @Test
    void testGetParameterName() {
        assertEquals(SupportedParameters.APP_FEATURES, validator.getParameterName());
    }

    @Test
    void testGetContainerType() {
        assertEquals(Module.class, validator.getContainerType());
    }

}
