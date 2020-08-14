package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.Test;

class StartTimeoutParameterConverterTest {

    @Test
    void testConvertWithInvalidValueType() {
        assertThrows(SLException.class, () -> new StartTimeoutParameterConverter().convert(false));
    }

    @Test
    void testConvertWithInvalidValue() {
        assertThrows(SLException.class, () -> new StartTimeoutParameterConverter().convert(-1000));
    }

    @Test
    void testConvert() {
        int startTimeout = (int) new StartTimeoutParameterConverter().convert("1000");
        assertEquals(1000, startTimeout);
    }

}
