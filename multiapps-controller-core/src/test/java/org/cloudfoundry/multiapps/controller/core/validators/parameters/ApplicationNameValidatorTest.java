package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.ApplicationNameValidator;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Test;

public class ApplicationNameValidatorTest {

    private static final String NAMESPACE = "foo";
    private static final String APPLICATION_NAME = "bar";
    private static final Map<String, Object> CONTEXT_APPLY_NAMESPACE = MapUtil.asMap(SupportedParameters.APPLY_NAMESPACE,
                                                                                     new Boolean(true));
    private static final Map<String, Object> CONTEXT_DO_NOT_APPLY_NAMESPACE = MapUtil.asMap(SupportedParameters.APPLY_NAMESPACE,
                                                                                            new Boolean(false));

    @Test
    public void testCorrectionWithNoNamespaces() {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(null, false);
        String result = (String) applicationNameValidator.attemptToCorrect(APPLICATION_NAME, CONTEXT_DO_NOT_APPLY_NAMESPACE);
        assertEquals(APPLICATION_NAME, result);
    }

    @Test
    public void testCorrectionWithNamespaces() {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, true);
        String result = (String) applicationNameValidator.attemptToCorrect(APPLICATION_NAME, CONTEXT_APPLY_NAMESPACE);
        assertEquals(String.format("%s" + Constants.NAMESPACE_SEPARATOR + "%s", NAMESPACE, APPLICATION_NAME), result);
    }

    @Test
    public void testCorrectionWithExplicitApplyNamespaceFalse() {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, true);
        String result = (String) applicationNameValidator.attemptToCorrect(APPLICATION_NAME, CONTEXT_DO_NOT_APPLY_NAMESPACE);
        assertEquals(APPLICATION_NAME, result);
    }

    @Test
    public void testCorrectionWithInvalidApplicationName() {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, false);
        assertThrows(ContentException.class,
                     () -> applicationNameValidator.attemptToCorrect(Collections.emptyList(), CONTEXT_DO_NOT_APPLY_NAMESPACE));
    }

    @Test
    public void testValidation() {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, false);
        assertFalse(applicationNameValidator.isValid(APPLICATION_NAME, CONTEXT_DO_NOT_APPLY_NAMESPACE));
    }

    @Test
    public void testGetContainerType() {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, false);
        assertEquals(Module.class, applicationNameValidator.getContainerType());
    }

    @Test
    public void testGetParameterName() {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, false);
        assertEquals(SupportedParameters.APP_NAME, applicationNameValidator.getParameterName());
    }

    @Test
    public void testCanCorrect() {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, false);
        assertTrue(applicationNameValidator.canCorrect());
    }

}
