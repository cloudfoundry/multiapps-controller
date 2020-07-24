package org.cloudfoundry.multiapps.controller.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.util.NameUtil.NameRequirements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class NameUtilTest {

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
        assertTrue(containerName.matches(NameRequirements.CONTAINER_NAME_PATTERN));
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
        assertTrue(xsAppName1.matches(NameRequirements.XS_APP_NAME_PATTERN));
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
