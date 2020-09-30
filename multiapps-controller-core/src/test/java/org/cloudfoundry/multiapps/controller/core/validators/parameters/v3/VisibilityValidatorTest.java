package org.cloudfoundry.multiapps.controller.core.validators.parameters.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class VisibilityValidatorTest {

    private final VisibilityValidator validator = new VisibilityValidator();

    static Stream<Arguments> testValidate() {
        return Stream.of(
    // @formatter:off
            // (0)
            Arguments.of("[{\"org\":\"org1\",\"space\":\"space1\"}]", true),
            // (1)
            Arguments.of("[{\"org\":\"org1\",\"space\":\"space1\"},{\"org\":\"org2\",\"space\":\"space2\"}]", true),
            // (2) Test with only org without space:
            Arguments.of("[{\"org\":\"org1\"}]", true),
            // (3) Test with random object
            Arguments.of("\"randomString\"", false),
            // (4) Test with only space without org:
            Arguments.of("[{\"space\":\"space1\"}]", false),
            // (5) Not a List
            Arguments.of("{\"org\":\"org1\",\"space\":\"space1\"}", false),
            // (6) Test with org that is integer:
            Arguments.of("[{\"org\": 3,\"space\":\"space1\"}]", false),
            // (7) Test with space that is integer:
            Arguments.of("[{\"org\":\"org1\",\"space\": 3}]", false),
            // (8) Test with org and space that are integers:
            Arguments.of("[{\"org\": 3,\"space\": 3}]", false)
    // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testValidate(String visibleTargets, boolean isValid) {
        Object visibleTargetsObject = JsonUtil.fromJson(visibleTargets, Object.class);
        assertEquals(isValid, validator.isValid(visibleTargetsObject, null));
    }

    @Test
    void testGetParameterName() {
        assertEquals("visibility", validator.getParameterName());
    }

    @Test
    void testGetContainerType() {
        assertTrue(validator.getContainerType()
                            .isAssignableFrom(ProvidedDependency.class));
    }
}
