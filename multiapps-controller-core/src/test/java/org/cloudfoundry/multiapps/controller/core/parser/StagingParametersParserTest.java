package org.cloudfoundry.multiapps.controller.core.parser;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.LifecycleType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import static org.cloudfoundry.multiapps.controller.core.Messages.BUILDPACKS_NOT_ALLOWED_WITH_DOCKER;
import static org.cloudfoundry.multiapps.controller.core.Messages.BUILDPACKS_REQUIRED_FOR_CNB;
import static org.cloudfoundry.multiapps.controller.core.Messages.DOCKER_INFO_NOT_ALLOWED_WITH_LIFECYCLE;
import static org.cloudfoundry.multiapps.controller.core.Messages.DOCKER_INFO_REQUIRED;
import static org.cloudfoundry.multiapps.controller.core.Messages.UNSUPPORTED_LIFECYCLE_VALUE;
import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.BUILDPACK;
import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.BUILDPACKS;
import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.DOCKER;
import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.LIFECYCLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StagingParametersParserTest {

    public static final String INVALID_VALUE = "invalid_value";
    public static final String CNB = "cnb";
    public static final String CUSTOM_BUILDPACK_URL = "custom-buildpack-url";
    public static final String IMAGE = "image";
    public static final String CLOUDFOUNDRY_TEST_APP = "cloudfoundry/test-app";
    public static final String SOME_BUILDPACK = "some-buildpack";
    private StagingParametersParser parser;
    private List<Map<String, Object>> parametersList;

    @BeforeEach
    void setup() {
        parser = new StagingParametersParser();
        parametersList = new ArrayList<>();
    }

    @Test
    void testValidateLifecycleWithCnbAndValidBuildpacks() {
        parametersList.add(mapOf(LIFECYCLE, CNB));
        parametersList.add(mapOf(BUILDPACKS, List.of(CUSTOM_BUILDPACK_URL)));

        Staging staging = parser.parse(parametersList);

        assertNotNull(staging);
        assertEquals(LifecycleType.CNB, staging.getLifecycleType());
        assertNotNull(staging.getBuildpacks());
        assertEquals(1, staging.getBuildpacks()
                               .size());
        assertEquals(CUSTOM_BUILDPACK_URL, staging.getBuildpacks()
                                                  .get(0));
    }

    @Test
    void testValidateLifecycleWithCnbAndNoBuildpacks() {
        parametersList.add(mapOf(LIFECYCLE, CNB));

        ContentException exception = assertThrows(ContentException.class, () -> parser.parse(parametersList));
        assertEquals(BUILDPACKS_REQUIRED_FOR_CNB, exception.getMessage());
    }

    @Test
    void testValidateLifecycleWithDockerAndValidDockerInfo() {
        parametersList.add(getDockerParams());

        Staging staging = parser.parse(parametersList);

        assertNotNull(staging);
        assertEquals(LifecycleType.DOCKER, staging.getLifecycleType());
        assertNotNull(staging.getDockerInfo());
    }

    @Test
    void testValidateLifecycleWithDockerAndBuildpacksProvided() {
        parametersList.add(getDockerParams());
        parametersList.add(mapOf(BUILDPACKS, List.of(SOME_BUILDPACK)));

        ContentException exception = assertThrows(ContentException.class, () -> parser.parse(parametersList));
        assertEquals(BUILDPACKS_NOT_ALLOWED_WITH_DOCKER, exception.getMessage());
    }

    @Test
    void testValidateLifecycleWithBuildpackAndNoBuildpacks() {
        parametersList.add(mapOf(LIFECYCLE, BUILDPACK));

        Staging staging = parser.parse(parametersList);

        assertNotNull(staging);
        assertEquals(LifecycleType.BUILDPACK, staging.getLifecycleType());

        List<String> buildpacks = staging.getBuildpacks();
        assertTrue(CollectionUtils.isEmpty(buildpacks));
    }

    @Test
    void testValidateLifecycleWithInvalidLifecycleValue() {
        parametersList.add(mapOf(LIFECYCLE, INVALID_VALUE));

        ContentException exception = assertThrows(ContentException.class, () -> parser.parse(parametersList));
        assertEquals(MessageFormat.format(UNSUPPORTED_LIFECYCLE_VALUE, INVALID_VALUE), exception.getMessage());
    }

    @Test
    void testValidateLifecycleWithDockerAndNoDockerInfo() {
        parametersList.add(mapOf(LIFECYCLE, DOCKER));

        ContentException exception = assertThrows(ContentException.class, () -> parser.parse(parametersList));
        assertEquals(DOCKER_INFO_REQUIRED, exception.getMessage());
    }

    @Test
    void testValidateDockerInfoWithNonDockerLifecycle() {
        parametersList.add(mapOf(LIFECYCLE, CNB));
        parametersList.add(mapOf(BUILDPACKS, List.of(SOME_BUILDPACK)));
        parametersList.add(getDockerParams());

        ContentException exception = assertThrows(ContentException.class, () -> parser.parse(parametersList));
        assertEquals(MessageFormat.format(DOCKER_INFO_NOT_ALLOWED_WITH_LIFECYCLE, LifecycleType.CNB), exception.getMessage());
    }

    @Test
    void testValidateWithAllParametersMissing() {
        Staging staging = parser.parse(parametersList);

        assertNotNull(staging);
        assertNull(staging.getLifecycleType());
        assertTrue(CollectionUtils.isEmpty(staging.getBuildpacks()));
        assertNull(staging.getDockerInfo());
    }

    private static Map<String, Object> mapOf(String key, Object value) {
        return Collections.singletonMap(key, value);
    }

    private static Map<String, Object> getDockerParams() {
        return Map.of(LIFECYCLE, DOCKER, DOCKER, Map.of(IMAGE, CLOUDFOUNDRY_TEST_APP));
    }

}
