package com.sap.cloud.lm.sl.cf.core.security.serialization;

import static com.sap.cloud.lm.sl.common.util.TestUtil.test;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class SecureSerializationFacadeTest {

    private String objectLocation;
    private Collection<String> sensitiveElementNames;
    private Class<?> clasz;
    private Expectation expectation;

    public SecureSerializationFacadeTest(String objectLocation, Collection<String> sensitiveElementNames, Class<?> expectedType,
                                         Expectation expectation) {
        this.objectLocation = objectLocation;
        this.sensitiveElementNames = sensitiveElementNames;
        this.clasz = expectedType;
        this.expectation = expectation;
    }

    @Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Sensitive information should be detected in element keys:
            {
                "unsecured-object-00.json", Arrays.asList("pwd", "pass"), Object.class, new Expectation(Expectation.Type.RESOURCE, "secured-object-00.json"),
            },
            // (1) Sensitive information should be detected in element keys, but there's a typo in one of the keys:
            {
                "unsecured-object-01.json", Arrays.asList("pwd", "pass"), Object.class, new Expectation(Expectation.Type.RESOURCE, "secured-object-01.json"),
            },
            // (2) Sensitive information should be detected in element values:
            {
                "unsecured-object-02.json", Arrays.asList("pwd", "pass"), Object.class, new Expectation(Expectation.Type.RESOURCE, "secured-object-02.json"),
            },
            // (3) Sensitive information should be detected in element values:
            {
                "unsecured-object-03.json", Arrays.asList(), DeploymentDescriptor.class, new Expectation(Expectation.Type.RESOURCE, "secured-object-03.json"),
            },
            // (4) Sensitive information should be detected in element values:
            {
                "unsecured-object-04.json", Arrays.asList(), DeploymentDescriptor.class, new Expectation(Expectation.Type.RESOURCE, "secured-object-04.json"),
            },
// @formatter:on
        });
    }

    @Test
    public void testToJson() throws Exception {
        Object object = JsonUtil.fromJson(TestUtil.getResourceAsString(objectLocation, getClass()), clasz);
        test(() -> {
            return new SecureSerializationFacade().setFormattedOutput(true)
                                                  .setSensitiveElementNames(sensitiveElementNames)
                                                  .toJson(object);
        }, expectation, getClass());
    }

}
