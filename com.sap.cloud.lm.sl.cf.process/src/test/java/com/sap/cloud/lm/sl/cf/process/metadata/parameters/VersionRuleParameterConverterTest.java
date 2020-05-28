package com.sap.cloud.lm.sl.cf.process.metadata.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.common.SLException;

public class VersionRuleParameterConverterTest {

    @Test
    public void testConvertWithInvalidValueType() {
        assertThrows(SLException.class, () -> new VersionRuleParameterConverter().convert(false));
    }

    @Test
    public void testConvertWithInvalidValue() {
        assertThrows(SLException.class, () -> new VersionRuleParameterConverter().convert("foo"));
    }

    @Test
    public void testConvert() {
        String versionRule = (String) new VersionRuleParameterConverter().convert("ALL");
        assertEquals("ALL", versionRule);
    }

}
