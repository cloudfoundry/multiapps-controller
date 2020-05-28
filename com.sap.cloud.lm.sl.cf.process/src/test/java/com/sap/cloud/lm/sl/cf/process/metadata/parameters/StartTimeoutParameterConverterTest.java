package com.sap.cloud.lm.sl.cf.process.metadata.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.common.SLException;

public class StartTimeoutParameterConverterTest {

    @Test
    public void testConvertWithInvalidValueType() {
        assertThrows(SLException.class, () -> new StartTimeoutParameterConverter().convert(false));
    }

    @Test
    public void testConvertWithInvalidValue() {
        assertThrows(SLException.class, () -> new StartTimeoutParameterConverter().convert(-1000));
    }

    @Test
    public void testConvert() {
        int startTimeout = (int) new StartTimeoutParameterConverter().convert("1000");
        assertEquals(1000, startTimeout);
    }

}
