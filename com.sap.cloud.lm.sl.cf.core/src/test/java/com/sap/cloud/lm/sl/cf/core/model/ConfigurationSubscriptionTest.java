package com.sap.cloud.lm.sl.cf.core.model;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.XmlUtil;

@RunWith(Parameterized.class)
public class ConfigurationSubscriptionTest {

    private final String jsonContent, xmlContent;

    public ConfigurationSubscriptionTest(String jsonInput, String expectedXml) throws IOException {
        this.jsonContent = TestUtil.getResourceAsString(jsonInput, getClass());
        this.xmlContent = TestUtil.getResourceAsString(expectedXml, getClass());
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "configuration-subscription.json", "configuration-subscription.xml",
            },
// @formatter:on
        });
    }

    @Test
    public void testMarshlling() {
        ConfigurationSubscription configurationSubscription = JsonUtil.fromJson(jsonContent, ConfigurationSubscription.class);
        assertEquals(xmlContent, XmlUtil.toXml(configurationSubscription, true));
    }

    @Test
    public void testUnmarshlling() {
        ConfigurationSubscription configurationSubscription = XmlUtil.fromXml(xmlContent, ConfigurationSubscription.class);
        assertEquals(jsonContent, JsonUtil.toJson(configurationSubscription, true));
    }
}
