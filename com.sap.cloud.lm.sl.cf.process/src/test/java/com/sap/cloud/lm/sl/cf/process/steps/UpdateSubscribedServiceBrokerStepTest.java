package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.GenericArgumentMatcher;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class UpdateSubscribedServiceBrokerStepTest extends AbstractStepTest<UpdateSubscribedServiceBrokerStep> {

    private StepInput input;
    private String expectedExceptionMessage;
    private String warningMessage;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) With an application that matches to an existing service broker
            {
                "update-subscribed-service-broker-input-00.json", null, null
            },
            // (1) With an application that does not-matches to an existing service broker
            {
                "update-subscribed-service-broker-input-01.json", null, "Service broker with name \"test-broker\" does not exist"
            },
            // (2) With an application that has no password defined
            {
                "update-subscribed-service-broker-input-02.json", "Missing service broker password for application \"test-application\"", null
            },
            // (3) With an application that broker does not match any existing broker
            {
                "update-subscribed-service-broker-input-03.json", null, "Service broker with name \"test-broker-which-does-not-exists\" does not exist"
            },
            // (4) With an application which broker was deleted
            {
                "update-subscribed-service-broker-input-04.json", null, "Service broker with name \"test-broker\" does not exist"
            },
            
// @formatter:on
        });
    }

    public UpdateSubscribedServiceBrokerStepTest(String inputLocation, String expectedExceptionMessage, String warningMessage)
        throws ParsingException, IOException {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, UpdateSubscribedServiceBrokerStepTest.class),
            StepInput.class);
        this.expectedExceptionMessage = expectedExceptionMessage;
        this.warningMessage = warningMessage;
    }

    @Before
    public void setUp() {
        prepareClient();
        prepareContext();
        if (expectedExceptionMessage != null) {
            expectedException.expect(SLException.class);
            expectedException.expectMessage(expectedExceptionMessage);
        }
    }

    private void prepareClient() {
        Mockito.when(client.getServiceBroker(Mockito.anyString(), Mockito.eq(false))).thenReturn(null);
        if (input.brokerApplication.brokerName.equals(input.brokerFromClient.name)) {
            Mockito.when(client.getServiceBroker(input.brokerFromClient.name, false)).thenReturn(input.brokerFromClient.toServiceBroker());
        }
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX, 0);
        List<CloudApplication> brokers = new ArrayList<>();
        brokers.add(input.brokerApplication.toCloudApplication());
        StepsUtil.setUpdatedServiceBrokerSubscribers(context, brokers);
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        validateExecution();
    }

    private void validateExecution() {
        if (warningMessage != null) {
            Mockito.verify(stepLogger).warn(warningMessage);
        } else {
            CloudServiceBroker expectedBroker = new CloudServiceBroker(null, input.brokerApplication.brokerName,
                input.brokerApplication.brokerUrl, input.brokerApplication.brokerUser, input.brokerApplication.brokerPassword);
            Mockito.verify(client).updateServiceBroker(Mockito.argThat(GenericArgumentMatcher.forObject(expectedBroker)));
        }
    }

    @Override
    protected UpdateSubscribedServiceBrokerStep createStep() {
        return new UpdateSubscribedServiceBrokerStep();
    }

    private static class StepInput {
        SimpleApplication brokerApplication;
        SimpleBroker brokerFromClient;
    }

    private static class SimpleApplication {
        String name;
        String brokerName;
        String brokerUrl;
        String brokerPassword;
        String brokerUser;

        CloudApplicationExtended toCloudApplication() {
            CloudApplicationExtended application = new CloudApplicationExtended(null, name);
            Map<String, Object> brokerDetails = getBrokerDetails();
            application.setEnv(MapUtil.asMap(com.sap.cloud.lm.sl.cf.core.Constants.ENV_DEPLOY_ATTRIBUTES, brokerDetails));
            return application;
        }

        private Map<String, Object> getBrokerDetails() {
            return new HashMap<String, Object>() {
                private static final long serialVersionUID = 1L;

                {
                    put(SupportedParameters.CREATE_SERVICE_BROKER, true);
                    put(SupportedParameters.SERVICE_BROKER_NAME, brokerName);
                    put(SupportedParameters.SERVICE_BROKER_URL, brokerUrl);
                    put(SupportedParameters.SERVICE_BROKER_USER, brokerUser);
                    put(SupportedParameters.SERVICE_BROKER_PASSWORD, brokerPassword);
                }
            };
        }
    }

    private static class SimpleBroker {
        String name;

        CloudServiceBroker toServiceBroker() {
            return (name == null) ? null : new CloudServiceBroker(null, name, null, null);
        }
    }
}
