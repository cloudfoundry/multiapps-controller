package com.sap.cloud.lm.sl.cf.core.helpers;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Test;

import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class ApplicationAttributesGetterTest {

    @Test
    public void testGetAttributeWithValidTypes() throws Exception {
        ApplicationAttributesGetter attributesGetter = getApplicationAttributesGetterForApplicationInJsonFile(
            "application-with-valid-deploy-attributes.json");
        assertEquals("username", attributesGetter.getAttribute("service-broker-username", String.class));
        assertEquals("password", attributesGetter.getAttribute("service-broker-password", String.class));
        assertEquals("default-url", attributesGetter.getAttribute("service-broker-url", String.class, "default-url"));
        assertEquals(true, attributesGetter.getAttribute("create-service-broker", Boolean.class));
        assertEquals(MapUtil.asMap("foo", "bar"), attributesGetter.getAttribute("env", Map.class));
    }

    @Test
    public void testGetAttributeWithInvalidTypes() throws Exception {
        ApplicationAttributesGetter attributesGetter = getApplicationAttributesGetterForApplicationInJsonFile(
            "application-with-invalid-deploy-attributes.json");
        TestUtil.test(() -> attributesGetter.getAttribute("create-service-broker", Boolean.class),
            "E:Attribute \"create-service-broker\" of application \"foo\" is of type java.lang.String instead of java.lang.Boolean!",
            getClass());
    }

    private ApplicationAttributesGetter getApplicationAttributesGetterForApplicationInJsonFile(String jsonFileLocation) throws Exception {
        String applicationJson = TestUtil.getResourceAsString(jsonFileLocation, getClass());
        CloudApplication application = JsonUtil.fromJson(applicationJson, CloudApplication.class);
        return ApplicationAttributesGetter.forApplication(application);
    }

}
