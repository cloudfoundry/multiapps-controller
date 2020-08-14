package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.Test;

class VersionRuleParameterConverterTest {

    @Test
    void testConvertWithInvalidValueType() {
        assertThrows(SLException.class, () -> new VersionRuleParameterConverter().convert(false));
    }

    @Test
    void testConvertWithInvalidValue() {
        assertThrows(SLException.class, () -> new VersionRuleParameterConverter().convert("foo"));
    }

    @Test
    void testConvert() {
        String versionRule = (String) new VersionRuleParameterConverter().convert("ALL");
        assertEquals("ALL", versionRule);
    }

}
