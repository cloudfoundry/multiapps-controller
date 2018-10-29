package com.sap.cloud.lm.sl.cf.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.cf.v1.ResourceType;
import com.sap.cloud.lm.sl.cf.core.cf.v1.SpecialResourceTypesRequiredParameters;
import com.sap.cloud.lm.sl.common.ContentException;

class SpecialResourceTypesRequiredParametersUtilTest {

    @Test
    void getRequiredParametersForManagedService() {
        ResourceType resourceType = ResourceType.MANAGED_SERVICE;
        Set<String> expectedRequiredParameters = SpecialResourceTypesRequiredParameters.getRequiredParameters(resourceType);
        Set<String> actualRequiredParameters = SpecialResourceTypesRequiredParametersUtil.getRequiredParameters(resourceType);
        assertEquals(expectedRequiredParameters, actualRequiredParameters);
    }
    
    @Test
    void getRequiredParametersForUserProvidedService() {
        ResourceType resourceType = ResourceType.USER_PROVIDED_SERVICE;
        Set<String> expectedRequiredParameters = SpecialResourceTypesRequiredParameters.getRequiredParameters(resourceType);
        Set<String> actualRequiredParameters = SpecialResourceTypesRequiredParametersUtil.getRequiredParameters(resourceType);
        assertEquals(expectedRequiredParameters, actualRequiredParameters);
    }
    
    @Test
    void getRequiredParametersForExistingService() {
        ResourceType resourceType = ResourceType.EXISTING_SERVICE;
        Set<String> expectedRequiredParameters = SpecialResourceTypesRequiredParameters.getRequiredParameters(resourceType);
        Set<String> actualRequiredParameters = SpecialResourceTypesRequiredParametersUtil.getRequiredParameters(resourceType);
        assertEquals(expectedRequiredParameters, actualRequiredParameters);
    }
    
    @Test
    void getRequiredParametersForExistingServiceKey() {
        ResourceType resourceType = ResourceType.EXISTING_SERVICE_KEY;
        Set<String> expectedRequiredParameters = SpecialResourceTypesRequiredParameters.getRequiredParameters(resourceType);
        Set<String> actualRequiredParameters = SpecialResourceTypesRequiredParametersUtil.getRequiredParameters(resourceType);
        assertEquals(expectedRequiredParameters, actualRequiredParameters);
    }
    
    @Test
    void checkRequiredParametersForManagedServiceWithNoParameters() {
        Map<String, Object> dummyParameters = new HashMap<>();
        ResourceType resourceType = ResourceType.MANAGED_SERVICE;
        Assertions.assertThrows(ContentException.class, () -> SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(resourceType, dummyParameters));
    }
    
    @Test
    void checkRequiredParametersForManagedServiceWithMissingParameter() {
        Map<String, Object> dummyParameters = new HashMap<>();
        dummyParameters.put("service", new Object());
        ResourceType resourceType = ResourceType.MANAGED_SERVICE;
        Assertions.assertThrows(ContentException.class, () -> SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(resourceType, dummyParameters));
    }

    @Test
    void checkRequiredParametersForExistingServiceWithNoParameters() {
        Map<String, Object> dummyParameters = new HashMap<>();
        ResourceType resourceType = ResourceType.EXISTING_SERVICE;
        Assertions.assertDoesNotThrow(() -> SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(resourceType, dummyParameters));
    }
    
}
