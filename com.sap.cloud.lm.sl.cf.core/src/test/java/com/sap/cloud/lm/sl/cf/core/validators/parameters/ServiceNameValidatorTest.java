package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class ServiceNameValidatorTest {

    private static final String NAMESPACE = "foo";
    private static final String SERVICE_NAME = "bar";

    @Test
    public void testCorrectionWithNoNamespaces() {
        boolean useNamespaces = false;
        boolean useNamespacesForServices = true;
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, useNamespaces, useNamespacesForServices);
        String result = (String) serviceNameValidator.attemptToCorrect(SERVICE_NAME);
        assertEquals(SERVICE_NAME, result);
    }

    @Test
    public void testCorrectionWithNamespaces() {
        boolean useNamespaces = true;
        boolean useNamespacesForServices = true;
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, useNamespaces, useNamespacesForServices);
        String result = (String) serviceNameValidator.attemptToCorrect(SERVICE_NAME);
        assertEquals(String.format("%s.%s", NAMESPACE, SERVICE_NAME), result);
    }

    @Test
    public void testCorrectionWithInvalidServiceName() {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, true, true);
        assertThrows(ContentException.class, () -> serviceNameValidator.attemptToCorrect(Collections.emptyList()));
    }

    @Test
    public void testValidation() {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, true, true);
        assertFalse(serviceNameValidator.isValid(SERVICE_NAME));
    }

    @Test
    public void testGetContainerType() {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, true, true);
        assertEquals(Resource.class, serviceNameValidator.getContainerType());
    }

    @Test
    public void testGetParameterName() {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, true, true);
        assertEquals(SupportedParameters.SERVICE_NAME, serviceNameValidator.getParameterName());
    }

    @Test
    public void testCanCorrect() {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, true, true);
        assertTrue(serviceNameValidator.canCorrect());
    }

}
