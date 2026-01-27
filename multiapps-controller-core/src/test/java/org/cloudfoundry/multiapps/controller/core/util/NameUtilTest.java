package org.cloudfoundry.multiapps.controller.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil.NameRequirements;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameUtilTest {

    static Stream<Arguments> testGetNameWithProperLength() {
        return Stream.of(Arguments.of("foo", 3, "foo"), Arguments.of("foo", 5, "foo"),
                         Arguments.of("com.sap.cloud.lm.sl.xs2.core.test", 20, "com.sap.clou3726daf0"));
    }

    static Stream<Arguments> testCreateValidXsAppName() {
        return Stream.of(Arguments.of("sap_system_com.sap.cloud.lm.sl.xs2.deploy-service-database",
                                      "_com.sap.cloud.lm.sl.xs2.deploy-service-database"),
                         Arguments.of("sap_system", "_"), Arguments.of("com.sap.cloud.lm.sl.xs@.deploy-service-database",
                                                                       "com.sap.cloud.lm.sl.xs_.deploy-service-database"));
    }

    static Stream<Arguments> testComputeNamespacedNameWithLength() {
        return Stream.of(Arguments.of("name", "namespace", true, false, 100, "namespace-name"),
                         Arguments.of("name", "namespace", false, false, 100, "name"),
                         Arguments.of("this-name-is-exactly-34-chars-long", "namespace", false, false, 34,
                                      "this-name-is-exactly-34-chars-long"),
                         Arguments.of("this-name-is-exactly-34-chars-long", "namespace", true, false, 30, "namespace-this-name-is54080287"),
                         Arguments.of("this-name-is-exactly-34-chars-long", "at-this-point-only-namespace-present", true, false, 40,
                                      "at-this-point-only-namespace-pre7548697a"),
                         Arguments.of("this-name-is-exactly-34-chars-long", "same-length-namespace-but-less-limit", true, false, 30,
                                      "same-length-namespace-774ffaef"),
                         Arguments.of("this", "same-length-namespace-but-less-limit", true, true, 63,
                                      "this-same-length-namespace-but-less-limit"),
                         Arguments.of("this-idle", "same-length-namespace-but-less-limit", true, true, 63,
                                      "this-same-length-namespace-but-less-limit-idle"),
                         Arguments.of("this-green", "same-length-namespace-but-less-limit", true, true, 63,
                                      "this-same-length-namespace-but-less-limit-green"),
                         Arguments.of("this-blue", "same-length-namespace-but-less-limit", true, true, 63,
                                      "this-same-length-namespace-but-less-limit-blue"),
                         Arguments.of("long-long-name-long-long-name-long-long-name-long-long-name", "same-length-namespace-but-less-limit",
                                      true, true, 63, "long-long-na6c807a4d-same-length-namespace-but-less-limit"),
                         Arguments.of("long-long-name-long-long-name-long-long-name-long-long-name-blue",
                                      "same-length-namespace-but-less-limit", true, true, 63,
                                      "long-long-na18c93fda-same-length-namespace-but-less-limit-blue"),
                         Arguments.of("long-long-name-long-long-name-long-long-name-long-long-name-green",
                                      "same-length-namespace-but-less-limit", true, true, 63,
                                      "long-long-namea7af83-same-length-namespace-but-less-limit-green"),
                         Arguments.of("long-long-name-long-long-name-long-long-name-long-long-name-idle",
                                      "same-length-namespace-but-less-limit", true, true, 63,
                                      "long-long-na18cc4f54-same-length-namespace-but-less-limit-idle"),
                         Arguments.of("long-long-name-long-long-name-long-long-name-long-long-name", "limit", true, true, 63,
                                      "long-long-name-long-long-name-long-long-nam6c807a4d-limit"),
                         Arguments.of("long-long-name-long-long-name-long-long-name-long-long-name-blue", "limit", true, true, 63,
                                      "long-long-name-long-long-name-long-long-nam18c93fda-limit-blue"),
                         Arguments.of("long-long-name-long-long-name-long-long-name-long-long-name-green", "limit", true, true, 63,
                                      "long-long-name-long-long-name-long-long-name-a7af83-limit-green"),
                         Arguments.of("long-long-name-long-long-name-long-long-name-long-long-name-idle", "limit", true, true, 63,
                                      "long-long-name-long-long-name-long-long-nam18cc4f54-limit-idle"));
    }

    static Stream<Arguments> serviceInstanceNameCases() {
        return Stream.of(Arguments.of("resource-name", "service-name-from-param", "service-name-from-param"),
                         Arguments.of("resource-name-1", null, "resource-name-1"),
                         Arguments.of("resource-name-2", "   ", "resource-name-2"),
                         Arguments.of("resource-name-3", "", "resource-name-3"));
    }

    @ParameterizedTest
    @MethodSource
    void testGetNameWithProperLength(String name, int maxLength, String expectedName) {
        assertEquals(expectedName, NameUtil.getNameWithProperLength(name, maxLength));
    }

    @Test
    void testCreateValidContainerName() {
        String containerName = NameUtil.computeValidContainerName("initial", "initial",
                                                                  "com.sap.cloud.lm.sl.xs2.a.very.very.long.service.name.with.illegal.container.name.characters");
        assertEquals("INITIAL_INITIAL_COM_SAP_CLOUD_LM_SL_XS2_A_VERY_VERY_LONG3AC0B612", containerName);
        assertTrue(containerName.matches(NameRequirements.CONTAINER_NAME_PATTERN));
    }

    @ParameterizedTest
    @MethodSource
    void testCreateValidXsAppName(String serviceName, String expectedName) {
        String xsAppName1 = NameUtil.computeValidXsAppName(serviceName);
        assertEquals(expectedName, xsAppName1);
        assertTrue(xsAppName1.matches(NameRequirements.XS_APP_NAME_PATTERN));
    }

    @ParameterizedTest
    @MethodSource
    void testComputeNamespacedNameWithLength(String name, String namespace, boolean applyNamespace, boolean applyNamespaceAsSuffix,
                                             int maxLength, String expectedName) {
        assertEquals(expectedName,
                     NameUtil.computeNamespacedNameWithLength(name, namespace, applyNamespace, applyNamespaceAsSuffix, maxLength));
    }

    @ParameterizedTest
    @MethodSource
    void serviceInstanceNameCases(String resourceName, String serviceNameParam, String expected) {
        Resource resource = createResource(resourceName, serviceNameParam);
        String serviceInstanceName = NameUtil.getServiceInstanceNameOrDefault(resource);

        assertEquals(expected, serviceInstanceName);
    }

    private Resource createResource(String resourceName, String serviceInstanceName) {
        Resource resource = Resource.createV3();
        resource.setName(resourceName);

        Map<String, Object> params = new HashMap<>();
        if (serviceInstanceName != null) {
            params.put(SupportedParameters.SERVICE_NAME, serviceInstanceName);
        }
        resource.setParameters(params);

        return resource;
    }
}
