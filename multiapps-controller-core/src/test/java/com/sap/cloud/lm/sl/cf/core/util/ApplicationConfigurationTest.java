package com.sap.cloud.lm.sl.cf.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingFacade;
import com.sap.cloud.lm.sl.cf.core.configuration.Environment;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration.DatabaseType;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;

public class ApplicationConfigurationTest {

    // TODO - tests about masking of sensitive entries while logging
    // TODO - tests about parsing values of different types

    private static final String VCAP_APPLICATION_JSON_WITHOUT_SPACE_ID = "{\"instance_id\":\"d827d1f2-d4c1-462a-ab9d-5bf828706d63\","
        + "\"application_name\":\"deploy-service\"," + "\"started_at_timestamp\":\"1437740774167\","
        + "\"state_timestamp\":\"1437651427557\"," + "\"application_uris\":[\"sofd60245639a:12993\"],"
        + "\"uris\":[\"sofd60245639a:12993\"]}";
    private static final String VCAP_APPLICATION_JSON = "{\"instance_id\":\"d827d1f2-d4c1-462a-ab9d-5bf828706d63\","
        + "\"application_name\":\"deploy-service\"," + "\"space_id\":\"954229f5-4945-43eb-8acb-a8f07cc5a7f8\","
        + "\"started_at_timestamp\":\"1437740774167\"," + "\"state_timestamp\":\"1437651427557\","
        + "\"application_uris\":[\"sofd60245639a:12993\"]," + "\"uris\":[\"sofd60245639a:12993\"]}";

    @Mock
    private Environment environment;
    @Mock
    private AuditLoggingFacade auditLoggingFascade;
    @InjectMocks
    private ApplicationConfiguration configuration;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLoadDefaultsWithAnEmptyEnvironment() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> configuration.load());
        assertEquals(Messages.CONTROLLER_URL_NOT_SPECIFIED, e.getMessage());
    }

    @Test
    public void testGetCfControllerUrl() throws Exception {
        URL expectedControllerUrl = new URL("https://api.example.com");
        Map<String, String> vcapApplication = MapUtil.asMap("cf_api", expectedControllerUrl.toString());
        assertEquals(expectedControllerUrl, getControllerUrlWithVcapApplication(vcapApplication));
    }

    @Test
    public void testGetXsControllerUrl() throws Exception {
        URL expectedControllerUrl = new URL("https://localhost:30030");
        Map<String, String> vcapApplication = MapUtil.asMap("xs_api", expectedControllerUrl.toString());
        assertEquals(expectedControllerUrl, getControllerUrlWithVcapApplication(vcapApplication));
    }

    @Test
    public void testGetControllerUrlWithInvalidValue() {
        String invalidUrl = "blabla";
        Map<String, String> vcapApplication = MapUtil.asMap("cf_api", invalidUrl);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                                  () -> getControllerUrlWithVcapApplication(vcapApplication));
        e.printStackTrace();
        assertEquals(MessageFormat.format(Messages.INVALID_CONTROLLER_URL, invalidUrl), e.getMessage());
    }

    public URL getControllerUrlWithVcapApplication(Map<String, String> vcapApplication) throws Exception {
        String vcapApplicationJson = JsonUtil.toJson(vcapApplication);
        when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION)).thenReturn(vcapApplicationJson);
        ApplicationConfiguration testedConfiguration = new ApplicationConfiguration(environment);
        return testedConfiguration.getControllerUrl();
    }

    @Test
    public void testReadDBType() {
        DatabaseType expectedDBType = DatabaseType.POSTGRESQL;
        when(environment.getString(ApplicationConfiguration.CFG_DB_TYPE)).thenReturn(expectedDBType.toString());
        ApplicationConfiguration testedConfiguration = new ApplicationConfiguration(environment);
        assertEquals(expectedDBType, testedConfiguration.getDatabaseType());
    }

    @Test
    public void testGetSpaceIdWithNull() throws Exception {
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_ID, configuration.getSpaceId());
    }

    @Test
    public void testGetSpaceIdWithEmptyString() throws Exception {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION))
               .thenReturn("");
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_ID, configuration.getSpaceId());
    }

    @Test
    public void testGetSpaceIdWithInvalidJson() throws Exception {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION))
               .thenReturn("invalid");
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_ID, configuration.getSpaceId());
    }

    @Test
    public void testGetSpaceIdWithEmptyMap() throws Exception {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION))
               .thenReturn("{}");
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_ID, configuration.getSpaceId());
    }

    @Test
    public void testGetSpaceIdWithMissingSpaceId() throws Exception {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION))
               .thenReturn(VCAP_APPLICATION_JSON_WITHOUT_SPACE_ID);
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_ID, configuration.getSpaceId());
    }

    @Test
    public void testGetSpaceId() throws Exception {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION))
               .thenReturn(VCAP_APPLICATION_JSON);
        String spaceId = configuration.getSpaceId();
        assertEquals("954229f5-4945-43eb-8acb-a8f07cc5a7f8", spaceId);
    }

}
