package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sap.cloud.lm.sl.mta.model.v1_0.Module;

public class PortValidatorTest {

    private PortValidator validator = new PortValidator();

    @Test
    public void testValidate() {
        assertTrue(validator.validate(1));
        assertTrue(validator.validate(65535));
        assertTrue(validator.validate(30030));

        assertFalse(validator.validate(-1));
        assertFalse(validator.validate(-65535));
        assertFalse(validator.validate(+65536));

        assertFalse(validator.validate("30030"));
    }

    @Test
    public void testCanCorrect() {
        assertFalse(validator.canCorrect());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAttemptToCorrect() throws Exception {
        validator.attemptToCorrect(-1);
    }

    @Test
    public void testGetParameterName() {
        assertEquals("port", validator.getParameterName());
    }

    @Test
    public void testGetContainerType() {
        assertTrue(validator.getContainerType().isAssignableFrom(Module.class));
    }

}
