package com.sap.cloud.lm.sl.cf.core.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingFacade;
import com.sap.cloud.lm.sl.cf.core.configuration.Environment;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration.DatabaseType;

import jersey.repackaged.com.google.common.collect.ImmutableMap;

public class ApplicationConfigurationTest {

    // TODO - tests about masking of sensitive entries while logging
    // TODO - tests about parsing values of different types

    private static final String VCAP_APPLICATION_JSON_WITHOUT_SPACE_GUID = "{\"instance_id\":\"d827d1f2-d4c1-462a-ab9d-5bf828706d63\","
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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLoadDefaultsNoGivenValues() {
        configuration.load();
        assertEquals(DatabaseType.DEFAULTDB, configuration.getDatabaseType());

    }

    @Test
    public void testReadDBType() {
        DatabaseType expectedDBType = DatabaseType.POSTGRESQL;
        when(environment.getString(ApplicationConfiguration.CFG_DB_TYPE)).thenReturn(expectedDBType.toString());
        ApplicationConfiguration testedConfiguration = new ApplicationConfiguration(environment);
        assertEquals(expectedDBType, testedConfiguration.getDatabaseType());
    }

    @Test
    public void testLogFullConfig() {
        when(environment.getAllVariables()).thenReturn(ImmutableMap.of(ApplicationConfiguration.CFG_DB_TYPE, "POSTGRES"));
        ApplicationConfiguration testedConfig = new ApplicationConfiguration(environment) {
            protected AuditLoggingFacade getAuditLoggingFascade() {
                return auditLoggingFascade;
            }
        };
        testedConfig.logFullConfig();
    }

    @Test
    public void testGetSpaceGuidWithNull() throws Exception {
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_ID, configuration.getSpaceGuid());
    }

    @Test
    public void testGetSpaceGuidWithEmptyString() throws Exception {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION))
            .thenReturn("");
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_ID, configuration.getSpaceGuid());
    }

    @Test
    public void testGetSpaceGuidWithInvalidJson() throws Exception {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION))
            .thenReturn("invalid");
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_ID, configuration.getSpaceGuid());
    }

    @Test
    public void testGetSpaceGuidWithEmptyMap() throws Exception {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION))
            .thenReturn("{}");
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_ID, configuration.getSpaceGuid());
    }

    @Test
    public void testGetSpaceGuidWithMissingSpaceId() throws Exception {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION))
            .thenReturn(VCAP_APPLICATION_JSON_WITHOUT_SPACE_GUID);
        assertEquals(ApplicationConfiguration.DEFAULT_SPACE_ID, configuration.getSpaceGuid());
    }

    @Test
    public void testGetSpaceGuid() throws Exception {
        Mockito.when(environment.getString(ApplicationConfiguration.CFG_VCAP_APPLICATION))
            .thenReturn(VCAP_APPLICATION_JSON);
        String spaceGuid = configuration.getSpaceGuid();
        assertEquals("954229f5-4945-43eb-8acb-a8f07cc5a7f8", spaceGuid);
    }

}
