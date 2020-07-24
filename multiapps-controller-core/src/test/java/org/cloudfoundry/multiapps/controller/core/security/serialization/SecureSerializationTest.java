package org.cloudfoundry.multiapps.controller.core.security.serialization;

import java.util.Arrays;
import java.util.Collection;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SecureSerializationTest {

    private final Tester tester = Tester.forClass(getClass());

    private final String objectLocation;
    private final Class<?> classOfObject;
    private final Expectation expectation;

    public SecureSerializationTest(String objectLocation, Class<?> classOfObject, Expectation expectation) {
        this.objectLocation = objectLocation;
        this.classOfObject = classOfObject;
        this.expectation = expectation;
    }

    @Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Sensitive information should be detected in element keys:
            {
                "unsecured-object-00.json", Object.class, new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-00.json")),
            },
            // (1) Sensitive information should be detected in element keys, but there's a typo in one of the keys:
            {
                "unsecured-object-01.json", Object.class, new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-01.json")),
            },
            // (2) Sensitive information should be detected in element values:
            {
                "unsecured-object-02.json", Object.class, new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-02.json")),
            },
            // (3) Sensitive information should be detected in element values:
            {
                "unsecured-object-03.json", DeploymentDescriptor.class, new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-03.json")),
            },
            // (4) Sensitive information should be detected in element values:
            {
                "unsecured-object-04.json", DeploymentDescriptor.class, new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-04.json")),
            },
// @formatter:on
        });
    }

    @Test
    public void testToJson() {
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
