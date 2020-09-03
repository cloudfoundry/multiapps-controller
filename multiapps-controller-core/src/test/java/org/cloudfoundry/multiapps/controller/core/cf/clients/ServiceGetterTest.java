package org.cloudfoundry.multiapps.controller.core.cf.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ServiceGetterTest {

    private static final String SPACE_ID = "test-space-id";
    private static final String SERVICE_NAME = "test-service";

    @Mock
    private ServiceInstanceGetter serviceInstanceGetter;
    @Mock
    private UserProvidedServiceInstanceGetter userProvidedInstanceGetter;
    @Mock
    private CloudControllerClient client;

    private ServiceGetter serviceGetter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        serviceGetter = new ServiceGetter(serviceInstanceGetter, userProvidedInstanceGetter);
    }

    static Stream<Arguments> testGetServiceInstanceEntity() {
        return Stream.of(
        // @formatter:off
                         Arguments.of(Map.of("test-service-instance", "test-value"), null),
                         Arguments.of(Map.of("test-service-instance", "test-value"), Collections.emptyMap()),
                         Arguments.of(Collections.emptyMap(), Map.of("test-user-provided-instance", "test-value")),
                         Arguments.of(null, Map.of("test-user-provided-instance", "test-value"))
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGetServiceInstanceEntity(Map<String, Object> serviceInstanceGetterResponse,
                                      Map<String, Object> userProvidedInstanceGetterResponse) {
        prepareServiceGetters(serviceInstanceGetterResponse, userProvidedInstanceGetterResponse);

        Map<String, Object> serviceInstanceEntity = serviceGetter.getServiceInstanceEntity(client, SERVICE_NAME, SPACE_ID);

        if (MapUtils.isEmpty(serviceInstanceGetterResponse)) {
            assertEquals(userProvidedInstanceGetterResponse, serviceInstanceEntity);
            verify(userProvidedInstanceGetter).getServiceInstanceEntity(client, SERVICE_NAME, SPACE_ID);
            return;
        }
        assertEquals(serviceInstanceGetterResponse, serviceInstanceEntity);
        verify(serviceInstanceGetter).getServiceInstanceEntity(client, SERVICE_NAME, SPACE_ID);
    }

    private void prepareServiceGetters(Map<String, Object> serviceInstanceGetterResponse,
                                       Map<String, Object> userProvidedInstanceGetterResponse) {
        when(serviceInstanceGetter.getServiceInstanceEntity(any(), anyString(), anyString())).thenReturn(serviceInstanceGetterResponse);
        when(userProvidedInstanceGetter.getServiceInstanceEntity(any(), anyString(),
                                                                 anyString())).thenReturn(userProvidedInstanceGetterResponse);
    }

}
