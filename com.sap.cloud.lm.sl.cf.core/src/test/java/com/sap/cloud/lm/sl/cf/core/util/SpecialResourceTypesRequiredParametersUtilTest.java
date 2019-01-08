package com.sap.cloud.lm.sl.cf.core.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.cf.v2.ResourceType;
import com.sap.cloud.lm.sl.common.ContentException;

class SpecialResourceTypesRequiredParametersUtilTest {
    private String testServiceName = "testService";

    @Test
    void checkRequiredParametersForManagedServiceWithNoParameters() {
        Map<String, Object> dummyParameters = new HashMap<>();
        ResourceType resourceType = ResourceType.MANAGED_SERVICE;
        Assertions.assertThrows(ContentException.class,
            () -> SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(testServiceName, resourceType, dummyParameters));
    }

    @Test
    void checkRequiredParametersForManagedServiceWithMissingParameter() {
        Map<String, Object> dummyParameters = new HashMap<>();
        dummyParameters.put("service", new Object());
        ResourceType resourceType = ResourceType.MANAGED_SERVICE;
        Assertions.assertThrows(ContentException.class,
            () -> SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(testServiceName, resourceType, dummyParameters));
    }

    @Test
    void checkRequiredParametersForManagedServiceWithRequiredParameter() {
        Map<String, Object> dummyParameters = new HashMap<>();
        dummyParameters.put("service", new Object());
        dummyParameters.put("service-plan", new Object());
        ResourceType resourceType = ResourceType.MANAGED_SERVICE;
        Assertions.assertDoesNotThrow(
            () -> SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(testServiceName, resourceType, dummyParameters));
    }

    @Test
    void checkRequiredParametersForUserProvidedServiceWithNoParameters() {
        Map<String, Object> dummyParameters = new HashMap<>();
        ResourceType resourceType = ResourceType.USER_PROVIDED_SERVICE;
        Assertions.assertDoesNotThrow(
            () -> SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(testServiceName, resourceType, dummyParameters));
    }

    @Test
    void checkRequiredParametersForUserProvidedServiceWithRequiredParameter() {
        Map<String, Object> dummyParameters = new HashMap<>();
        dummyParameters.put("config", new Object());
        ResourceType resourceType = ResourceType.USER_PROVIDED_SERVICE;
        Assertions.assertDoesNotThrow(
            () -> SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(testServiceName, resourceType, dummyParameters));
    }

    @Test
    void checkRequiredParametersForExistingServiceWithNoParameters() {
        Map<String, Object> dummyParameters = new HashMap<>();
        ResourceType resourceType = ResourceType.EXISTING_SERVICE;
        Assertions.assertDoesNotThrow(
            () -> SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(testServiceName, resourceType, dummyParameters));
    }

}
