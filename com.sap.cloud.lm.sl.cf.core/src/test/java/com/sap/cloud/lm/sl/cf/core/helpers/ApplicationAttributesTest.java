package com.sap.cloud.lm.sl.cf.core.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class ApplicationAttributesTest {

    private static final String APP_NAME = "foo";

    @Test
    public void testGet() {
        ApplicationAttributes appAttributes = createApplicationAttributesFromJsonFile("application-with-valid-deploy-attributes.json");
        assertEquals("username", appAttributes.get("service-broker-username", String.class));
        assertEquals("password", appAttributes.get("service-broker-password", String.class));
        assertEquals("default-url", appAttributes.get("service-broker-url", String.class, "default-url"));
        assertEquals(true, appAttributes.get("create-service-broker", Boolean.class));
        assertEquals(MapUtil.asMap("foo", "bar"), appAttributes.get("env", Map.class));
        assertNull(appAttributes.get("non-existing-attribute", String.class));
    }

    @Test
    public void testGetWithInvalidType() {
        ApplicationAttributes appAttributes = createApplicationAttributesFromJsonFile("application-with-invalid-deploy-attributes.json");

        String attributeName = "create-service-broker";
        ParsingException e = assertThrows(ParsingException.class, () -> appAttributes.get(attributeName, Boolean.class));

        String expectedMessage = MessageFormat.format(Messages.ATTRIBUTE_0_OF_APP_1_IS_OF_TYPE_2_INSTEAD_OF_3, attributeName, APP_NAME,
                                                      String.class.getSimpleName(), Boolean.class.getSimpleName());
        assertEquals(expectedMessage, e.getMessage());
    }

    @Test
    public void testGetWithInvalidAttributes() {
        CloudApplication app = ImmutableCloudApplication.builder()
                                                        .name(APP_NAME)
                                                        .env(MapUtil.asMap("DEPLOY_ATTRIBUTES", "INVALID_JSON_OBJECT"))
                                                        .build();

        ParsingException e = assertThrows(ParsingException.class, () -> ApplicationAttributes.fromApplication(app));

        String expectedMessage = MessageFormat.format(Messages.COULD_NOT_PARSE_ATTRIBUTES_OF_APP_0, APP_NAME);
        assertEquals(expectedMessage, e.getMessage());
    }

    @Test
    public void testGetWithMissingAttributes() {
        CloudApplication app = ImmutableCloudApplication.builder()
                                                        .name(APP_NAME)
                                                        .build();
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app);

        assertNull(appAttributes.get("service-broker-url", String.class));
        assertEquals("default-url", appAttributes.get("service-broker-url", String.class, "default-url"));
    }

    @Test
    public void testGetWithNullAttributes() {
        CloudApplication app = ImmutableCloudApplication.builder()
                                                        .name(APP_NAME)
                                                        .env(MapUtil.asMap("DEPLOY_ATTRIBUTES", "null"))
                                                        .build();
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app);

        assertNull(appAttributes.get("service-broker-url", String.class));
        assertEquals("default-url", appAttributes.get("service-broker-url", String.class, "default-url"));
    }

    private ApplicationAttributes createApplicationAttributesFromJsonFile(String jsonFileLocation) {
        String applicationJson = TestUtil.getResourceAsString(jsonFileLocation, getClass());
        CloudApplication application = JsonUtil.fromJson(applicationJson, CloudApplication.class);
        return ApplicationAttributes.fromApplication(application);
    }

}
