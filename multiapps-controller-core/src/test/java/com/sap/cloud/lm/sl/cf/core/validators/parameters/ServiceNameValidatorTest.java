package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class ServiceNameValidatorTest {

    private static final String NAMESPACE = "foo";
    private static final String SERVICE_NAME = "bar";
    private static final Map<String, Object> CONTEXT_APPLY_NAMESPACE = MapUtil.asMap(SupportedParameters.APPLY_NAMESPACE,
                                                                                     new Boolean(true));
    private static final Map<String, Object> CONTEXT_DO_NOT_APPLY_NAMESPACE = MapUtil.asMap(SupportedParameters.APPLY_NAMESPACE,
                                                                                            new Boolean(false));

    @Test
    public void testCorrectionWithNoNamespaces() {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(null, false);
        String result = (String) serviceNameValidator.attemptToCorrect(SERVICE_NAME, CONTEXT_DO_NOT_APPLY_NAMESPACE);
        assertEquals(SERVICE_NAME, result);
    }

    @Test
    public void testCorrectionWithNamespaces() {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, true);
        String result = (String) serviceNameValidator.attemptToCorrect(SERVICE_NAME, CONTEXT_APPLY_NAMESPACE);
        assertEquals(String.format("%s" + Constants.NAMESPACE_SEPARATOR + "%s", NAMESPACE, SERVICE_NAME), result);
    }

    @Test
    public void testCorrectionWithInvalidServiceName() {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, false);
        assertThrows(ContentException.class,
                     () -> serviceNameValidator.attemptToCorrect(Collections.emptyList(), CONTEXT_DO_NOT_APPLY_NAMESPACE));
    }

    @Test
    public void testValidation() {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, false);
        assertFalse(serviceNameValidator.isValid(SERVICE_NAME, null));
    }

    @Test
    public void testGetContainerType() {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, false);
        assertEquals(Resource.class, serviceNameValidator.getContainerType());
    }

    @Test
    public void testGetParameterName() {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, false);
        assertEquals(SupportedParameters.SERVICE_NAME, serviceNameValidator.getParameterName());
    }

    @Test
    public void testCanCorrect() {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, false);
        assertTrue(serviceNameValidator.canCorrect());
    }

}
