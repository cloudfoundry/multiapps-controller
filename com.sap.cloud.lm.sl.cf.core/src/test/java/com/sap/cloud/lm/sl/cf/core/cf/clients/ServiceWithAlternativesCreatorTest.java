package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class ServiceWithAlternativesCreatorTest extends CloudServiceOperatorTest {

    private static final String CREATE_SERVICE_URL = "/v2/service_instances?accepts_incomplete=true";
    private static final String SPACE_ID = "TEST_SPACE";

    @Parameters(name="{0}")
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Service with credentials:
            {
                "service-01.json", null, null
            },
            // (1) Service without credentials:
            {
                "service-02.json", null, null
            },
            // (2) Service without label:
            {
                "service-03.json", "Service label must not be null", IllegalArgumentException.class
            },
            // (3) Service without name:
            {
                "service-04.json", "Service name must not be null", IllegalArgumentException.class
            },
            // (4) Service without plan:
            {
                "service-05.json", "Service plan must not be null", IllegalArgumentException.class
            },
            // (5) Service plan doesn't exist:
            {
                "service-06.json", "Could not create service instance \"foo\". Service plan \"v3.4-extra-large\" from service offering \"mongodb\" was not found.", CloudOperationException.class
            },
            // (6) Service offering doesn't exist:
            {
                "service-07.json", "Could not create service instance \"foo\". Service plan \"v2.0-sp3\" from service offering \"hana\" was not found.", CloudOperationException.class
            },
            // (7) Service has defined alternatives and default offering is matching:
            {
                "service-08.json",  null, null
            },
            // (8) Service has defined alternatives and one of them is used, because the default offering does not exist:
            {
                "service-09.json",  null, null
            },
            // (9) Service has defined alternatives and one of them is used, because creating with the default offering fails:
            {
                "service-10.json",  "Service \"foo\" could not be created because all attempt(s) to use service offerings \"[postgresql, postgresql-trial]\" failed", SLException.class
            },
            // (10) Service has defined alternatives, but none of them exist:
            {
                "service-11.json",  "Service \"foo\" could not be created because none of the service offering(s) \"[mysql, mysql-trial]\" match with existing service offerings or provide service plan \"v5.7-small\"" , SLException.class
            },
            // (11) Service has defined alternatives that exist, but do not provide the required plan:
            {
                "service-12.json",  "Service \"foo\" could not be created because none of the service offering(s) \"[postgresql, postgresql-trial]\" match with existing service offerings or provide service plan \"v9.6-small\"" , SLException.class
            },
            // (12) Service has defined alternatives and one of them is used, because the default offering does not provide the required service plan:
            {
                "service-13.json",  null, null
            },
            // (13) Service has defined alternatives, but creating a service fails with all of them:
            {
                "service-14.json",  "Service \"foo\" could not be created because all attempt(s) to use service offerings \"[postgresql, postgresql-trial]\" failed", SLException.class
            },
            // (14) Service with tags:
            {
                "service-15.json",  null, null
            },
            // (15) Service has defined alternatives, but the creation fails with 401 Unauthorized:
            {
                "service-16.json", "401 Unauthorized", CloudOperationException.class
            },
// @formatter:on
        });
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private UserMessageLogger userMessageLogger;
    
    private ServiceCreator serviceCreator;
    private ServiceWithAlternativesCreator serviceWithAlternativesCreator;

    private Input input;
    private String expectedExceptionMessage;
    private Class<? extends RuntimeException> expectedExceptionType;

    public ServiceWithAlternativesCreatorTest(String inputLocation, String expected, Class<? extends RuntimeException> expectedExceptionClass)
        throws ParsingException, IOException {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, ServiceWithAlternativesCreatorTest.class), Input.class);
        this.expectedExceptionMessage = expected;
        this.expectedExceptionType = expectedExceptionClass;
    }

    @Before
    public void setUp() {
        serviceCreator = new ServiceCreator(getMockedRestTemplateFactory());
        serviceWithAlternativesCreator = new ServiceWithAlternativesCreator(serviceCreator, userMessageLogger);
        setUpException();
        setUpServiceRequests();
    }

    private void setUpServiceRequests() {
        Mockito.reset(getMockedRestTemplate());
        for (Exchange exchange : input.expectedExchanges) {
            if (exchange.responseCode == 201) {
                continue;
            }
            HttpStatus httpStatusCode = HttpStatus.valueOf(exchange.responseCode);
            Mockito.when(getMockedRestTemplate().exchange(Matchers.eq(getControllerUrl() + CREATE_SERVICE_URL), Matchers.eq(HttpMethod.POST), Matchers.any(), Matchers.eq(String.class)))
                .thenThrow(new CloudOperationException(httpStatusCode));
        }
    }

    private void setUpException() {
        if (expectedExceptionMessage != null) {
            expectedException.expect(expectedExceptionType);
            expectedException.expectMessage(expectedExceptionMessage);
        }
    }

    @Test
    public void testExecuteServiceOperation() {
        serviceWithAlternativesCreator.createService(getMockedClient(), input.service, SPACE_ID);

        validateRestCall();
    }

    private void validateRestCall() {
        for (Exchange exchange : input.expectedExchanges) {
            Mockito.verify(getMockedRestTemplate())
                .exchange(Matchers.eq(getControllerUrl() + CREATE_SERVICE_URL), Matchers.eq(HttpMethod.POST), Matchers.any(), Matchers.eq(String.class));
        }
    }

    private static class Input {

        private CloudServiceExtended service;
        private List<Exchange> expectedExchanges = new ArrayList<>();

    }

    private static class Exchange {

        private Map<String, Object> requestBody;
        private int responseCode = 201;

    }

}
