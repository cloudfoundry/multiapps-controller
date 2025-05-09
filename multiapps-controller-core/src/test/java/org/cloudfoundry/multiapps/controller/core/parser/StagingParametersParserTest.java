package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StagingParametersParserTest {

    private StagingParametersParser parser;
    private List<Map<String, Object>> parametersList;

    @BeforeEach
    void setup() {
        parser = new StagingParametersParser();
        parametersList = new ArrayList<>();
    }

    @Test
    void testValidateLifecycleWithCnbAndValidBuildpacks() {
        parametersList.add(Collections.singletonMap("lifecycle", "cnb"));
        parametersList.add(Collections.singletonMap("buildpacks", List.of("custom-buildpack-url")));
        assertDoesNotThrow(() -> parser.parse(parametersList));
    }

    @Test
    void testValidateLifecycleWithCnbAndNoBuildpacks() {
        parametersList.add(Collections.singletonMap(SupportedParameters.LIFECYCLE, "cnb"));
        Exception exception = assertThrows(SLException.class, () -> parser.parse(parametersList));
        assertEquals("Buildpacks must be provided when lifecycle is set to 'cnb'.", exception.getMessage());
    }

    @Test
    void testValidateLifecycleWithDockerAndValidDockerInfo() {
        parametersList.add(getDockerParams());
        assertDoesNotThrow(() -> parser.parse(parametersList));
    }

    @Test
    void testValidateLifecycleWithDockerAndBuildpacksProvided() {
        parametersList.add(getDockerParams());
        parametersList.add(Collections.singletonMap("buildpacks", List.of("some-buildpack")));

        Exception exception = assertThrows(SLException.class, () -> parser.parse(parametersList));
        assertEquals("Buildpacks must not be provided when lifecycle is set to 'docker'.", exception.getMessage());
    }

    @Test
    void testValidateLifecycleWithBuildpackAndNoBuildpacks() {
        parametersList.add(Collections.singletonMap("lifecycle", "buildpack"));
        assertDoesNotThrow(() -> parser.parse(parametersList));
    }

    @Test
    void testValidateLifecycleWithInvalidLifecycleValue() {
        parametersList.add(Collections.singletonMap("lifecycle", "invalid_value"));
        Exception exception = assertThrows(SLException.class, () -> parser.parse(parametersList));
        assertEquals("Unsupported lifecycle value: invalid_value", exception.getMessage());
    }

    private static Map<String, Object> getDockerParams() {
        return Map.of(SupportedParameters.LIFECYCLE, "docker", SupportedParameters.DOCKER, Map.of("image", "cloudfoundry/test-app"));
    }

}
