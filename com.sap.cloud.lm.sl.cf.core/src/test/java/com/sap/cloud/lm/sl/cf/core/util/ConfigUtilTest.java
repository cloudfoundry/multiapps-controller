package com.sap.cloud.lm.sl.cf.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConfigUtilTest {

    private static final String VCAP_APPLICATION_JSON = "{\"instance_id\":\"d827d1f2-d4c1-462a-ab9d-5bf828706d63\","
        + "\"application_name\":\"deploy-service\"," + "\"started_at_timestamp\":\"1437740774167\","
        + "\"state_timestamp\":\"1437651427557\"," + "\"application_uris\":[\"sofd60245639a:12993\"],"
        + "\"uris\":[\"sofd60245639a:12993\"]}";
    private static final String VCAP_APPLICATION_JSON2 = "{\"instance_id\":\"d827d1f2-d4c1-462a-ab9d-5bf828706d63\","
        + "\"application_name\":\"deploy-service\"," + "\"space_id\":\"954229f5-4945-43eb-8acb-a8f07cc5a7f8\","
        + "\"started_at_timestamp\":\"1437740774167\"," + "\"state_timestamp\":\"1437651427557\","
        + "\"application_uris\":[\"sofd60245639a:12993\"]," + "\"uris\":[\"sofd60245639a:12993\"]}";

    @Test
    public void testGetSpaceGuidWithNull() throws Exception {
        assertEquals(ConfigurationUtil.DEFAULT_SPACE_ID, ConfigurationUtil.getSpaceGuid(null));
    }

    @Test
    public void testGetSpaceGuidWithEmptyString() throws Exception {
        assertEquals(ConfigurationUtil.DEFAULT_SPACE_ID, ConfigurationUtil.getSpaceGuid(""));
    }

    @Test
    public void testGetSpaceGuidWithInvalidJson() throws Exception {
        assertEquals(ConfigurationUtil.DEFAULT_SPACE_ID, ConfigurationUtil.getSpaceGuid("invalid"));
    }

    @Test
    public void testGetSpaceGuidWithEmptyMap() throws Exception {
        assertEquals(ConfigurationUtil.DEFAULT_SPACE_ID, ConfigurationUtil.getSpaceGuid("{}"));
    }

    @Test
    public void testGetSpaceGuidWithMissingSpaceId() throws Exception {
        assertEquals(ConfigurationUtil.DEFAULT_SPACE_ID, ConfigurationUtil.getSpaceGuid(VCAP_APPLICATION_JSON));
    }

    @Test
    public void testGetSpaceGuid() throws Exception {
        String spaceGuid = ConfigurationUtil.getSpaceGuid(VCAP_APPLICATION_JSON2);
        assertEquals("954229f5-4945-43eb-8acb-a8f07cc5a7f8", spaceGuid);
    }

}
