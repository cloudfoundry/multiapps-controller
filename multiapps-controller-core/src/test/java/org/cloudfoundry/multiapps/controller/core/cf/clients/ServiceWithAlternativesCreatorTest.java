package org.cloudfoundry.multiapps.controller.core.cf.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution.ExecutionState;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class ServiceWithAlternativesCreatorTest extends CloudServiceOperatorTest {

    @Mock
    private UserMessageLogger userMessageLogger;
    private ServiceWithAlternativesCreator serviceWithAlternativesCreator;
    private Input input;
    private String expectedExceptionMessage;
    private Class<? extends RuntimeException> expectedExceptionType;

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
                                    () -> serviceWithAlternativesCreator.createService(getMockedClient(), input.actualService),
                                    expectedExceptionMessage);
            return;
        }
        CloudControllerClient mockClient = getMockedClient();
        MethodExecution<String> actualMethodExecution = serviceWithAlternativesCreator.createService(mockClient, input.actualService);
        assertEquals(ExecutionState.EXECUTING, actualMethodExecution.getState());
        int callsForAllOfferings = input.actualService.getAlternativeLabels()
                                                      .isEmpty() ? 0 : 1;
        Mockito.verify(mockClient, Mockito.times(callsForAllOfferings))
               .getServiceOfferings();
        Mockito.verify(mockClient)
               .createServiceInstance(input.expectedService);
    }

    private void throwExceptionIfNeeded() {
        if (input.errorStatusCode != null) {
            Mockito.doThrow(new CloudOperationException(HttpStatus.resolve(input.errorStatusCode)))
                   .when(getMockedClient())
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
