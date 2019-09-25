package com.sap.cloud.lm.sl.cf.core.cf.clients;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class ServiceWithAlternativesCreatorTest extends CloudServiceOperatorTest {

    private static final String CREATE_SERVICE_URL = "/v2/service_instances?accepts_incomplete=true";
    private static final String SPACE_ID = "TEST_SPACE";
    @Mock
    private UserMessageLogger userMessageLogger;
    private ServiceCreator serviceCreator;
    private ServiceWithAlternativesCreator serviceWithAlternativesCreator;
    private Input input;
    private String expectedExceptionMessage;
    private Class<? extends RuntimeException> expectedExceptionType;

    // @formatter:off
    private static Stream<Arguments> testExecuteServiceOperation() {
        return Stream.of(
                Arguments.of("service-01.json", null, null),
                Arguments.of("service-02.json", null, null),
                Arguments.of("service-03.json", "Service label must not be null", IllegalArgumentException.class),
                Arguments.of("service-04.json", "Service name must not be null", IllegalArgumentException.class),
                Arguments.of("service-05.json", "Service plan must not be null", IllegalArgumentException.class),
                Arguments.of("service-06.json", "Could not create service instance \"foo\". Service plan \"v3.4-extra-large\" from service offering \"mongodb\" was not found.", CloudOperationException.class),
                Arguments.of("service-07.json", "Could not create service instance \"foo\". Service plan \"v2.0-sp3\" from service offering \"hana\" was not found.", CloudOperationException.class),
                Arguments.of("service-08.json",  null, null),
                Arguments.of("service-09.json",  null, null),
                Arguments.of("service-10.json",  "Service \"foo\" could not be created because all attempt(s) to use service offerings \"[postgresql, postgresql-trial]\" failed", SLException.class),
                Arguments.of("service-11.json",  "Service \"foo\" could not be created because none of the service offering(s) \"[mysql, mysql-trial]\" match with existing service offerings or provide service plan \"v5.7-small\"" , SLException.class),
                Arguments.of("service-12.json",  "Service \"foo\" could not be created because none of the service offering(s) \"[postgresql, postgresql-trial]\" match with existing service offerings or provide service plan \"v9.6-small\"" , SLException.class),
                Arguments.of("service-13.json",  null, null),
                Arguments.of("service-14.json",  "Service \"foo\" could not be created because all attempt(s) to use service offerings \"[postgresql, postgresql-trial]\" failed", SLException.class),
                Arguments.of("service-15.json",  null, null),
                Arguments.of("service-16.json", "401 Unauthorized", CloudOperationException.class),
                Arguments.of("service-17.json", "400 Bad Request", CloudOperationException.class)
        );
    }
    // @formatter:on

    private void setUpServiceRequests() {
        Mockito.reset(getMockedRestTemplate());
        for (Exchange exchange : input.expectedExchanges) {
            if (exchange.responseCode == HttpStatus.CREATED.value()) {
                continue;
            }
            HttpStatus httpStatusCode = HttpStatus.valueOf(exchange.responseCode);
            Mockito.when(getMockedRestTemplate().exchange(ArgumentMatchers.eq(getControllerUrl() + CREATE_SERVICE_URL),

                                                          ArgumentMatchers.eq(HttpMethod.POST), any(), ArgumentMatchers.eq(String.class)))
                   .thenThrow(new CloudOperationException(httpStatusCode));
        }
    }

    @ParameterizedTest
    @MethodSource
    public void testExecuteServiceOperation(String inputLocation, String expected,
                                            Class<? extends RuntimeException> expectedExceptionClass) {
        initializeComponents(inputLocation, expected, expectedExceptionClass);
        if (expectedExceptionClass != null) {
            Assertions.assertThrows(expectedExceptionType,
                                    () -> serviceWithAlternativesCreator.createService(getMockedClient(), input.service, SPACE_ID),
                                    expectedExceptionMessage);
            return;
        }
        serviceWithAlternativesCreator.createService(getMockedClient(), input.service, SPACE_ID);
        validateRestCall();
    }

    private void throwExceptionOnExistingService(String newServiceName) {
        getMockedClient().getServices()
                         .stream()
                         .map(CloudEntity::getName)
                         .forEach(serviceName -> {
                             if (Objects.equals(serviceName, newServiceName)) {
                                 Mockito.doThrow(new CloudOperationException(HttpStatus.BAD_REQUEST))
                                        .when(getMockedRestTemplate())
                                        .exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), (Class<String>) any());
                             }
                         });
    }

    private void initializeComponents(String inputLocation, String expected, Class<? extends RuntimeException> expectedExceptionClass) {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, ServiceWithAlternativesCreatorTest.class), Input.class);
        this.expectedExceptionMessage = expected;
        this.expectedExceptionType = expectedExceptionClass;
        serviceCreator = new ServiceCreator(getMockedRestTemplateFactory());
        serviceWithAlternativesCreator = new ServiceWithAlternativesCreator(serviceCreator, userMessageLogger);
        setUpServiceRequests();
        throwExceptionOnExistingService(input.service.getName());
    }

    private void validateRestCall() {
        for (Exchange exchange : input.expectedExchanges) {
            Mockito.verify(getMockedRestTemplate())
                   .exchange(ArgumentMatchers.eq(getControllerUrl() + CREATE_SERVICE_URL), ArgumentMatchers.eq(HttpMethod.POST), any(),
                             ArgumentMatchers.eq(String.class));
        }
    }

    private static class Input {

        private CloudServiceExtended service;
        private List<Exchange> expectedExchanges = new ArrayList<>();

    }

    private static class Exchange {

        private Map<String, Object> requestBody;
        private int responseCode = HttpStatus.CREATED.value();

    }

}
