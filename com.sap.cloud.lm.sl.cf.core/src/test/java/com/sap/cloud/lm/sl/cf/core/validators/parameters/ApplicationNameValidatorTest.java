package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.Module;

public class ApplicationNameValidatorTest {

    private static final String NAMESPACE = "foo";
    private static final String APPLICATION_NAME = "bar";

    @Test
    public void testCorrectionWithNoNamespaces() {
        boolean useNamespaces = false;
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, false);
        String result = (String) applicationNameValidator.attemptToCorrect(APPLICATION_NAME);
        assertEquals(APPLICATION_NAME, result);
    }

    @Test
    public void testCorrectionWithNamespaces() {
        boolean useNamespaces = true;
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, true);
        String result = (String) applicationNameValidator.attemptToCorrect(APPLICATION_NAME);
        assertEquals(String.format("%s.%s", NAMESPACE, APPLICATION_NAME), result);
    }

    @Test
    public void testCorrectionWithInvalidApplicationName() {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, true);
        assertThrows(ContentException.class, () -> applicationNameValidator.attemptToCorrect(Collections.emptyList()));
    }

    @Test
    public void testValidation() {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, true);
        assertFalse(applicationNameValidator.isValid(APPLICATION_NAME));
    }

    @Test
    public void testGetContainerType() {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, true);
        assertEquals(Module.class, applicationNameValidator.getContainerType());
    }

    @Test
    public void testGetParameterName() {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, true);
        assertEquals(SupportedParameters.APP_NAME, applicationNameValidator.getParameterName());
    }

    @Test
    public void testCanCorrect() {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, true);
        assertTrue(applicationNameValidator.canCorrect());
    }

}
