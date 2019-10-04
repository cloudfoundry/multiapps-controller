package com.sap.cloud.lm.sl.cf.core.model;

import static org.junit.Assert.assertEquals;

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

    private final String jsonResource, xmlResource;

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

    public ConfigurationSubscriptionTest(String jsonResource, String xmlResource) {
        this.jsonResource = loadResource(jsonResource);
        this.xmlResource = loadResource(xmlResource);
    }

    private String loadResource(String resource) {
        return TestUtil.getResourceAsStringWithoutCarriageReturns(resource, getClass());
    }

    @Test
    public void testMarshlling() {
        ConfigurationSubscription subscription = JsonUtil.fromJson(jsonResource, ConfigurationSubscription.class);
        String actualXml = TestUtil.removeCarriageReturns(XmlUtil.toXml(subscription, true));
        assertEquals(xmlResource, actualXml);
    }

    @Test
    public void testUnmarshlling() {
        ConfigurationSubscription subscription = XmlUtil.fromXml(xmlResource, ConfigurationSubscription.class);
        String actualJson = TestUtil.removeCarriageReturns(JsonUtil.toJson(subscription, true));
        assertEquals(jsonResource, actualJson);
    }

}
