package org.cloudfoundry.multiapps.controller.core.cf.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution.ExecutionState;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceOffering;

class ServiceWithAlternativesCreatorTest {
 
    private static final String SERVICE_OFFERINGS_RESPONSE_PATH = "service-offerings.json";

    @Mock
    private UserMessageLogger userMessageLogger;
    @Mock
    private CloudControllerClient client;
    private ServiceWithAlternativesCreator serviceWithAlternativesCreator;
    private Input input;
    private String expectedExceptionMessage;
    private Class<? extends RuntimeException> expectedExceptionType;

    @BeforeEach
    public void prepareClients() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        prepareClient();
    }

    private void prepareClient() {
        List<CloudServiceOffering> serviceOfferings = loadServiceOfferingsFromFile(SERVICE_OFFERINGS_RESPONSE_PATH);
        Mockito.when(client.getServiceOfferings())
               .thenReturn(serviceOfferings);
    }

    private List<CloudServiceOffering> loadServiceOfferingsFromFile(String filePath) {
        String serviceOfferingsJson = TestUtil.getResourceAsString(filePath, getClass());
        return JsonUtil.fromJson(serviceOfferingsJson, new TypeReference<>() {
        });
    }
    
    // @formatter:off
     static Stream<Arguments> testExecuteServiceOperation() {
        return Stream.of(
                Arguments.of("service-01.json", null, null),
                Arguments.of("service-02.json", null, null),
                Arguments.of("service-03.json", "Service label must not be null", IllegalArgumentException.class),
                Arguments.of("service-04.json", "Service name must not be null", IllegalArgumentException.class),
                Arguments.of("service-05.json", "Service plan must not be null", IllegalArgumentException.class),
                Arguments.of("service-06.json", "401 Unauthorized", CloudOperationException.class),
                Arguments.of("service-07.json", "400 Bad Request", CloudOperationException.class),
                Arguments.of("service-08.json",  null, null),
                Arguments.of("service-09.json",  null, null),
                Arguments.of("service-10.json",  "Service \"foo\" could not be created because all attempt(s) to use service offerings \"[postgresql, postgresql-trial]\" failed", SLException.class),
                Arguments.of("service-11.json",  "Service \"foo\" could not be created because none of the service offering(s) \"[mysql, mysql-trial]\" match with existing service offerings or provide service plan \"v5.7-small\"" , SLException.class),
                Arguments.of("service-12.json",  "Service \"foo\" could not be created because none of the service offering(s) \"[postgresql, postgresql-trial]\" match with existing service offerings or provide service plan \"v9.6-small\"" , SLException.class),
                Arguments.of("service-13.json",  null, null),
                Arguments.of("service-14.json",  "Service \"foo\" could not be created because all attempt(s) to use service offerings \"[postgresql, postgresql-trial]\" failed", SLException.class),
                Arguments.of("service-15.json",  null, null)
        );
    }
    // @formatter:on

    @ParameterizedTest
    @MethodSource
    void testExecuteServiceOperation(String inputLocation, String expected, Class<? extends RuntimeException> expectedExceptionClass) {
        initializeComponents(inputLocation, expected, expectedExceptionClass);
        if (expectedExceptionClass != null) {
            Assertions.assertThrows(expectedExceptionType,
                                    () -> serviceWithAlternativesCreator.createService(client, input.actualService),
                                    expectedExceptionMessage);
            return;
        }
        MethodExecution<String> actualMethodExecution = serviceWithAlternativesCreator.createService(client, input.actualService);
        assertEquals(ExecutionState.EXECUTING, actualMethodExecution.getState());
        int callsForAllOfferings = input.actualService.getAlternativeLabels()
                                                      .isEmpty() ? 0 : 1;
        Mockito.verify(client, Mockito.times(callsForAllOfferings))
               .getServiceOfferings();
        Mockito.verify(client)
               .createServiceInstance(input.expectedService);
    }

    private void throwExceptionIfNeeded() {
        if (input.errorStatusCode != null) {
            Mockito.doThrow(new CloudOperationException(HttpStatus.resolve(input.errorStatusCode)))
                   .when(client)
                   .createServiceInstance(any());
        }
    }

    private void initializeComponents(String inputLocation, String expected, Class<? extends RuntimeException> expectedExceptionClass) {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, ServiceWithAlternativesCreatorTest.class), Input.class);
        this.expectedExceptionMessage = expected;
        this.expectedExceptionType = expectedExceptionClass;
        serviceWithAlternativesCreator = new ServiceWithAlternativesCreator(userMessageLogger);
        throwExceptionIfNeeded();
    }

    private static class Input {
        private CloudServiceInstanceExtended actualService;
        private CloudServiceInstanceExtended expectedService;
        private Integer errorStatusCode;
    }

}
