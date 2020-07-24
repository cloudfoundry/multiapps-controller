package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.process.metadata.parameters.VersionRuleParameterConverter;
import org.junit.jupiter.api.Test;

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
