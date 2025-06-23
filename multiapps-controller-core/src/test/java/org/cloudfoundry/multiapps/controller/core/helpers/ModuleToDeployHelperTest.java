package org.cloudfoundry.multiapps.controller.core.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleToDeployHelperTest {

    private ModuleToDeployHelper helper;

    @BeforeEach
    void setUp() {
        helper = new ModuleToDeployHelper();
    }

    static Stream<Arguments> skipDeployParameters() {
        // @formatter:off
        return Stream.of(Arguments.of(null, false),
                         Arguments.of(true, true),
                         Arguments.of(false, false),
                         Arguments.of("true", true),
                         Arguments.of("false", false),
                         Arguments.of("invalid", false));
        // @formatter:on
    }

    @ParameterizedTest
    @MethodSource("skipDeployParameters")
    void testShouldSkipDeploy(Object skipDeployValue, boolean expected) {
        Module module = Module.createV3();
        if (skipDeployValue != null) {
            Map<String, Object> params = new HashMap<>();
            params.put(SupportedParameters.SKIP_DEPLOY, skipDeployValue);
            module.setParameters(params);
        }
        assertEquals(expected, helper.shouldSkipDeploy(module));
    }
}
