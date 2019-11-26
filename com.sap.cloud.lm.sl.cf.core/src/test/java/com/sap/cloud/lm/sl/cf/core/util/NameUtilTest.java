package com.sap.cloud.lm.sl.cf.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloud.lm.sl.cf.core.util.NameUtil.NameRequirements;

public class NameUtilTest {

    @Test
    public void testIsValidXsAppNameWhenNamesAreValid() {
        testIsValidName(Arrays.asList("Fo0", "foo.bar", "foo_bar", "foo-bar", "//foo\\"), NameRequirements.XS_APP_NAME_PATTERN, true);
    }

    @Test
    public void testIsValidXsAppNameWhenNamesAreInvalid() {
        String longName = StringUtils.repeat("f", NameRequirements.XS_APP_NAME_MAX_LENGTH + 1);

        testIsValidName(Arrays.asList("sap_system", "sap_system_1", "foo&&bar", "foo bar", longName), NameRequirements.XS_APP_NAME_PATTERN,
                        false);
    }

    @Test
    public void testIsValidContainerNameWhenNamesAreValid() {
        testIsValidName(Arrays.asList("FOO", "FOO_BAR", "1_FOO", "FOO_1_BAR"), NameRequirements.CONTAINER_NAME_PATTERN, true);
    }

    @Test
    public void testIsValidContainerNameWhenNamesAreInvalid() {
        String longName = StringUtils.repeat("F", NameRequirements.CONTAINER_NAME_MAX_LENGTH + 1);

        testIsValidName(Arrays.asList("foo", "FOO BAR", "1-FOO", "FOO_&_BAR", "_FOO", " ", longName),
                        NameRequirements.CONTAINER_NAME_PATTERN, false);
    }

    private void testIsValidName(List<String> names, String namePattern, boolean expectedResult) {
        for (String name : names) {
            assertEquals(expectedResult, NameUtil.isValidName(name, namePattern));
        }
    }

    public static Stream<Arguments> testGetNameWithProperLength() {
        return Stream.of(
        // @formatter:off
                         Arguments.of("foo", 3, "foo"),
                         Arguments.of("foo", 5, "foo"),
                         Arguments.of("com.sap.cloud.lm.sl.xs2.core.test", 20, "com.sap.clou3726daf0")
                         // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testGetNameWithProperLength(String name, int maxLength, String expectedName) {
        assertEquals(expectedName, NameUtil.getNameWithProperLength(name, maxLength));
    }

    @Test
    public void testCreateValidContainerName() {
        String containerName = NameUtil.computeValidContainerName("initial", "initial",
                                                                  "com.sap.cloud.lm.sl.xs2.a.very.very.long.service.name.with.illegal.container.name.characters");
        assertEquals("INITIAL_INITIAL_COM_SAP_CLOUD_LM_SL_XS2_A_VERY_VERY_LONG3AC0B612", containerName);
        assertTrue(NameUtil.isValidName(containerName, NameRequirements.CONTAINER_NAME_PATTERN));
    }

    public static Stream<Arguments> testCreateValidXsAppName() {
        return Stream.of(
        // @formatter:off
                         Arguments.of("sap_system_com.sap.cloud.lm.sl.xs2.deploy-service-database", "_com.sap.cloud.lm.sl.xs2.deploy-service-database"),
                         Arguments.of("sap_system", "_"),
                         Arguments.of("com.sap.cloud.lm.sl.xs@.deploy-service-database", "com.sap.cloud.lm.sl.xs_.deploy-service-database")
                         // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCreateValidXsAppName(String serviceName, String expectedName) {
        String xsAppName1 = NameUtil.computeValidXsAppName(serviceName);
        assertEquals(expectedName, xsAppName1);
        assertTrue(NameUtil.isValidName(xsAppName1, NameRequirements.XS_APP_NAME_PATTERN));
    }

    public static Stream<Arguments> testComputeNamespacedNameWithLength() {
        return Stream.of(
        // @formatter:off
                         Arguments.of("name", "namespace", true, 100, "namespace-name"),
                         Arguments.of("name", "namespace", false, 100, "name"),
                         Arguments.of("this-name-is-exactly-34-chars-long", "namespace", false, 34, "this-name-is-exactly-34-chars-long"),
                         Arguments.of("this-name-is-exactly-34-chars-long", "namespace", true, 30, "namespace-this-name-is54080287"),
                         Arguments.of("this-name-is-exactly-34-chars-long", "at-this-point-only-namespace-present", true, 40, "at-this-point-only-namespace-pre7548697a"),
                         Arguments.of("this-name-is-exactly-34-chars-long", "same-length-namespace-but-less-limit", true, 30, "same-length-namespace-774ffaef")
                         // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testComputeNamespacedNameWithLength(String name, String namespace, boolean applyNamespace, int maxLength,
                                                    String expectedName) {
        assertEquals(expectedName, NameUtil.computeNamespacedNameWithLength(name, namespace, applyNamespace, maxLength));
    }

}
