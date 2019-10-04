package com.sap.cloud.lm.sl.cf.core.security.serialization;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class SecureSerializationFacadeTest {

    private final Tester tester = Tester.forClass(getClass());

    private String objectLocation;
    private Collection<String> sensitiveElementNames;
    private Class<?> classOfObject;
    private Expectation expectation;

    public SecureSerializationFacadeTest(String objectLocation, Class<?> classOfObject, Collection<String> sensitiveElementNames,
                                         Expectation expectation) {
        this.objectLocation = objectLocation;
        this.sensitiveElementNames = sensitiveElementNames;
        this.classOfObject = classOfObject;
        this.expectation = expectation;
    }

    @Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Sensitive information should be detected in element keys:
            {
                "unsecured-object-00.json", Object.class, Arrays.asList("pwd", "pass"), new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-00.json")),
            },
            // (1) Sensitive information should be detected in element keys, but there's a typo in one of the keys:
            {
                "unsecured-object-01.json", Object.class, Arrays.asList("pwd", "pass"), new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-01.json")),
            },
            // (2) Sensitive information should be detected in element values:
            {
                "unsecured-object-02.json", Object.class, Arrays.asList("pwd", "pass"), new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-02.json")),
            },
            // (3) Sensitive information should be detected in element values:
            {
                "unsecured-object-03.json", DeploymentDescriptor.class, Collections.emptyList(), new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-03.json")),
            },
            // (4) Sensitive information should be detected in element values:
            {
                "unsecured-object-04.json", DeploymentDescriptor.class, Collections.emptyList(), new Expectation(Expectation.Type.STRING, getResourceAsString("secured-object-04.json")),
            },
// @formatter:on
        });
    }

    @Test
    public void testToJson() {
        Object object = JsonUtil.fromJson(getResourceAsString(objectLocation), classOfObject);
        tester.test(() -> {
            String json = new SecureSerializationFacade().setFormattedOutput(true)
                                                         .setSensitiveElementNames(sensitiveElementNames)
                                                         .toJson(object);
            return TestUtil.removeCarriageReturns(json);
        }, expectation);
    }

    private static String getResourceAsString(String resource) {
        return TestUtil.getResourceAsStringWithoutCarriageReturns(resource, SecureSerializationFacadeTest.class);
    }

}
