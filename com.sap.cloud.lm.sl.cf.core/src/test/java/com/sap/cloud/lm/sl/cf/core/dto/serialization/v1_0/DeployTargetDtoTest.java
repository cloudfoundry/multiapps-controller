package com.sap.cloud.lm.sl.cf.core.dto.serialization.v1_0;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.dto.serialization.v1.DeployTargetDto;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.XmlUtil;

@RunWith(value = Parameterized.class)
public class DeployTargetDtoTest {

    protected final String jsonTargetContent, xmlTargetContent;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "platform-properties-v1.json", "platform-properties-v1.xml",
            },
// @formatter:on
        });
    }

    public DeployTargetDtoTest(String json, String xml) throws Throwable {
        this.jsonTargetContent = TestUtil.getResourceAsString(json, this.getClass());
        this.xmlTargetContent = TestUtil.getResourceAsString(xml, this.getClass());
    }

    protected Class<? extends com.sap.cloud.lm.sl.cf.core.dto.serialization.DeployTargetDto> getTestedClass() {
        return DeployTargetDto.class;
    }

    @Test
    public void testUnmarshalling() throws Exception {
        com.sap.cloud.lm.sl.cf.core.dto.serialization.DeployTargetDto properties = XmlUtil.fromXml(xmlTargetContent, getTestedClass());
        assertEquals("The deserialized deploy target xml is not as expected " + getTestedClass(), jsonTargetContent,
            JsonUtil.toJson(properties, true));
    }


    @Test
    public void testMarshalling() throws Exception {
        com.sap.cloud.lm.sl.cf.core.dto.serialization.DeployTargetDto properties = JsonUtil.fromJson(jsonTargetContent, getTestedClass());
        assertEquals("The serialized deploy target xml is not as expected" + getTestedClass(), xmlTargetContent,
            XmlUtil.toXml(properties, true));
    }

}
