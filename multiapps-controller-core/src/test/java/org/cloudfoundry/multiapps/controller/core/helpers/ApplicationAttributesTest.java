package org.cloudfoundry.multiapps.controller.core.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.util.Map;

import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.LifecycleType;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableLifecycle;

class ApplicationAttributesTest {

    private static final String APP_NAME = "foo";

    @Test
    void testGet() {
        ApplicationAttributes appAttributes = createApplicationAttributesFromJsonFile("application-with-valid-deploy-attributes.json");
        assertEquals("username", appAttributes.get("service-broker-username", String.class));
        assertEquals("password", appAttributes.get("service-broker-password", String.class));
        assertEquals("default-url", appAttributes.get("service-broker-url", String.class, "default-url"));
        assertEquals(true, appAttributes.get("create-service-broker", Boolean.class));
        assertEquals(Map.of("foo", "bar"), appAttributes.get("env", Map.class));
        assertNull(appAttributes.get("non-existing-attribute", String.class));
    }

    @Test
    void testGetWithInvalidType() {
        ApplicationAttributes appAttributes = createApplicationAttributesFromJsonFile("application-with-invalid-deploy-attributes.json");

        String attributeName = "create-service-broker";
        ParsingException e = assertThrows(ParsingException.class, () -> appAttributes.get(attributeName, Boolean.class));

        String expectedMessage = MessageFormat.format(Messages.ATTRIBUTE_0_OF_APP_1_IS_OF_TYPE_2_INSTEAD_OF_3, attributeName, APP_NAME,
                                                      String.class.getSimpleName(), Boolean.class.getSimpleName());
        assertEquals(expectedMessage, e.getMessage());
    }

    @Test
    void testGetWithInvalidAttributes() {
        CloudApplication app = ImmutableCloudApplication.builder()
                                                        .name(APP_NAME)
                                                        .build();
        Map<String, String> appEnv = Map.of("DEPLOY_ATTRIBUTES", "INVALID_JSON_OBJECT");

        ParsingException e = assertThrows(ParsingException.class, () -> ApplicationAttributes.fromApplication(app, appEnv));

        String expectedMessage = MessageFormat.format(Messages.COULD_NOT_PARSE_ATTRIBUTES_OF_APP_0, APP_NAME);
        assertEquals(expectedMessage, e.getMessage());
    }

    @Test
    void testGetWithMissingAttributes() {
        CloudApplication app = ImmutableCloudApplication.builder()
                                                        .name(APP_NAME)
                                                        .build();
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app, Map.of());

        assertNull(appAttributes.get("service-broker-url", String.class));
        assertEquals("default-url", appAttributes.get("service-broker-url", String.class, "default-url"));
    }

    @Test
    void testGetWithNullAttributes() {
        CloudApplication app = ImmutableCloudApplication.builder()
                                                        .name(APP_NAME)
                                                        .build();
        Map<String, String> appEnv = Map.of("DEPLOY_ATTRIBUTES", "null");
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app, appEnv);

        assertNull(appAttributes.get("service-broker-url", String.class));
        assertEquals("default-url", appAttributes.get("service-broker-url", String.class, "default-url"));
    }

    private ApplicationAttributes createApplicationAttributesFromJsonFile(String jsonFileLocation) {
        String applicationJson = TestUtil.getResourceAsString(jsonFileLocation, getClass());
        StepInput input = JsonUtil.fromJson(applicationJson, StepInput.class);
        CloudApplication app = ImmutableCloudApplication.builder()
                                                        .metadata(CloudMetadata.defaultMetadata())
                                                        .name(input.name)
                                                        .state(CloudApplication.State.STOPPED)
                                                        .lifecycle(ImmutableLifecycle.builder()
                                                                                     .type(LifecycleType.DOCKER)
                                                                                     .build())
                                                        .build();
        return ApplicationAttributes.fromApplication(app, input.env);
    }

    private static class StepInput {
        String name;
        Map<String, String> env;
    }

}
