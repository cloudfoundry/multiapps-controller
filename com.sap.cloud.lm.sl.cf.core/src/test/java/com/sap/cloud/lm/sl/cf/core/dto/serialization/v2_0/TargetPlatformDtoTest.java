package com.sap.cloud.lm.sl.cf.core.dto.serialization.v2_0;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.dto.serialization.v2.TargetPlatformDto;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.XmlUtil;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatform;

@RunWith(value = Parameterized.class)
public class TargetPlatformDtoTest {

    private final String json, xml;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "/platform/platform-v2.json", "/platform/platform-v2.xml",
            },
// @formatter:on
        });
    }

    public TargetPlatformDtoTest(String json, String xml) throws Throwable {
        this.json = TestUtil.getResourceAsString(json, TargetPlatformDtoTest.class);
        this.xml = TestUtil.getResourceAsString(xml, TargetPlatformDtoTest.class);
    }

    @Test
    public void testUnmarshalling() throws Exception {
        TargetPlatformDto platform = XmlUtil.fromXml(xml, TargetPlatformDto.class);

        String expected = json;
        String actual = JsonUtil.toJson(platform, true);

        assertEquals(expected, actual);
    }

    @Test
    public void testMarshalling() throws Exception {
        TargetPlatform platform = new ConfigurationParser().parsePlatformJson(json);

        String expected = xml;
        String actual = XmlUtil.toXml(new TargetPlatformDto(platform), true);

        assertEquals(expected, actual);
    }

}
