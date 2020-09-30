package org.cloudfoundry.multiapps.controller.core.security.serialization;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SecureSerializationTest {

    private final Tester tester = Tester.forClass(getClass());

    public static Stream<Arguments> testToJson() {
        return Stream.of(
// @formatter:off
            // (0) Sensitive information should be detected in element keys:
            Arguments.of("unsecured-object-00.json", Object.class, new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-00.json"))),
            // (1) Sensitive information should be detected in element keys, but there's a typo in one of the keys:
            Arguments.of("unsecured-object-01.json", Object.class, new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-01.json"))),
            // (2) Sensitive information should be detected in element values:
            Arguments.of("unsecured-object-02.json", Object.class, new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-02.json"))),
            // (3) Sensitive information should be detected in element values:
            Arguments.of("unsecured-object-03.json", DeploymentDescriptor.class, new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-03.json"))),
            // (4) Sensitive information should be detected in element values:
            Arguments.of("unsecured-object-04.json", DeploymentDescriptor.class, new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-04.json")))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testToJson(String objectLocation, Class<?> classOfObject, Expectation expectation) {
        Object object = JsonUtil.fromJson(getResourceAsString(objectLocation), classOfObject);
        tester.test(() -> {
            String json = SecureSerialization.toJson(object);
            return TestUtil.removeCarriageReturns(json);
        }, expectation);
    }

    private static String getResourceAsString(String resource) {
        return TestUtil.getResourceAsStringWithoutCarriageReturns(resource, SecureSerializationTest.class);
    }

}
