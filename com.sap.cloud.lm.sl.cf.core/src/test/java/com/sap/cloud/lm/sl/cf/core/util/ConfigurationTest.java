package com.sap.cloud.lm.sl.cf.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.helpers.Environment;

public class ConfigurationTest {

    private static final String VCAP_APPLICATION_JSON_WITHOUT_SPACE_GUID = "{\"instance_id\":\"d827d1f2-d4c1-462a-ab9d-5bf828706d63\","
        + "\"application_name\":\"deploy-service\"," + "\"started_at_timestamp\":\"1437740774167\","
        + "\"state_timestamp\":\"1437651427557\"," + "\"application_uris\":[\"sofd60245639a:12993\"],"
        + "\"uris\":[\"sofd60245639a:12993\"]}";
    private static final String VCAP_APPLICATION_JSON = "{\"instance_id\":\"d827d1f2-d4c1-462a-ab9d-5bf828706d63\","
        + "\"application_name\":\"deploy-service\"," + "\"space_id\":\"954229f5-4945-43eb-8acb-a8f07cc5a7f8\","
        + "\"started_at_timestamp\":\"1437740774167\"," + "\"state_timestamp\":\"1437651427557\","
        + "\"application_uris\":[\"sofd60245639a:12993\"]," + "\"uris\":[\"sofd60245639a:12993\"]}";

    private Environment environment = Mockito.mock(Environment.class);
    private Configuration configuration = new Configuration(environment);

    @Test
    public void testGetSpaceGuidWithNull() throws Exception {
        assertEquals(Configuration.DEFAULT_SPACE_ID, configuration.getSpaceGuid());
    }

    @Test
    public void testGetSpaceGuidWithEmptyString() throws Exception {
        Mockito.when(environment.getVariable(Configuration.CFG_VCAP_APPLICATION))
            .thenReturn("");
        assertEquals(Configuration.DEFAULT_SPACE_ID, configuration.getSpaceGuid());
    }

    @Test
    public void testGetSpaceGuidWithInvalidJson() throws Exception {
        Mockito.when(environment.getVariable(Configuration.CFG_VCAP_APPLICATION))
            .thenReturn("invalid");
        assertEquals(Configuration.DEFAULT_SPACE_ID, configuration.getSpaceGuid());
    }

    @Test
    public void testGetSpaceGuidWithEmptyMap() throws Exception {
        Mockito.when(environment.getVariable(Configuration.CFG_VCAP_APPLICATION))
            .thenReturn("{}");
        assertEquals(Configuration.DEFAULT_SPACE_ID, configuration.getSpaceGuid());
    }

    @Test
    public void testGetSpaceGuidWithMissingSpaceId() throws Exception {
        Mockito.when(environment.getVariable(Configuration.CFG_VCAP_APPLICATION))
            .thenReturn(VCAP_APPLICATION_JSON_WITHOUT_SPACE_GUID);
        assertEquals(Configuration.DEFAULT_SPACE_ID, configuration.getSpaceGuid());
    }

    @Test
    public void testGetSpaceGuid() throws Exception {
        Mockito.when(environment.getVariable(Configuration.CFG_VCAP_APPLICATION))
            .thenReturn(VCAP_APPLICATION_JSON);
        String spaceGuid = configuration.getSpaceGuid();
        assertEquals("954229f5-4945-43eb-8acb-a8f07cc5a7f8", spaceGuid);
    }

}
