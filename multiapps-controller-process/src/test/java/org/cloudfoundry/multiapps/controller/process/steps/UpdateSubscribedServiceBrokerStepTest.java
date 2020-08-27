package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBroker;
import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.GenericArgumentMatcher;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class UpdateSubscribedServiceBrokerStepTest extends SyncFlowableStepTest<UpdateServiceBrokerSubscriberStep> {

    private final StepInput input;
    private final String expectedExceptionMessage;
    private final String warningMessage;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

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
                "update-subscribed-service-broker-input-03.json", null, "Service broker with name \"test-broker-which-does-not-exist\" does not exist"
            },
            // (4) With an application which broker was deleted
            {
                "update-subscribed-service-broker-input-04.json", null, "Service broker with name \"test-broker\" does not exist"
            },
// @formatter:on
        });
    }

    public UpdateSubscribedServiceBrokerStepTest(String inputLocation, String expectedExceptionMessage, String warningMessage)
        throws ParsingException {
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
        Mockito.when(client.getServiceBroker(Mockito.anyString(), Mockito.eq(false)))
               .thenReturn(null);
        if (input.brokerApplication.brokerName.equals(input.brokerFromClient.name)) {
            Mockito.when(client.getServiceBroker(input.brokerFromClient.name, false))
                   .thenReturn(input.brokerFromClient.toServiceBroker());
        }
    }

    private void prepareContext() {
        context.setVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX, 0);
        List<CloudApplication> brokers = new ArrayList<>();
        brokers.add(input.brokerApplication.toCloudApplication());
        context.setVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS, brokers);
    }

    @Test
    public void testExecute() {
        step.execute(execution);

        validateExecution();
    }

    private void validateExecution() {
        if (warningMessage != null) {
            Mockito.verify(stepLogger)
                   .warn(warningMessage);
        } else {
            CloudServiceBroker expectedBroker = ImmutableCloudServiceBroker.builder()
                                                                           .name(input.brokerApplication.brokerName)
                                                                           .username(input.brokerApplication.brokerUsername)
                                                                           .password(input.brokerApplication.brokerPassword)
                                                                           .url(input.brokerApplication.brokerUrl)
                                                                           .build();
            Mockito.verify(client)
                   .updateServiceBroker(Mockito.argThat(GenericArgumentMatcher.forObject(expectedBroker)));
        }
    }

    @Override
    protected UpdateServiceBrokerSubscriberStep createStep() {
        return new UpdateServiceBrokerSubscriberStep();
    }

    private static class StepInput {
        SimpleApplication brokerApplication;
        SimpleBroker brokerFromClient;
    }

    private static class SimpleApplication {
        String name;
        String brokerName;
        String brokerPassword;
        String brokerUsername;
        String brokerUrl;

        CloudApplicationExtended toCloudApplication() {
            Map<String, Object> brokerDetails = getBrokerDetails();
            return ImmutableCloudApplicationExtended.builder()
                                                    .name(name)
                                                    .env(MapUtil.asMap(org.cloudfoundry.multiapps.controller.core.Constants.ENV_DEPLOY_ATTRIBUTES,
                                                                       JsonUtil.toJson(brokerDetails)))
                                                    .build();
        }

        private Map<String, Object> getBrokerDetails() {
            Map<String, Object> brokerDetails = new HashMap<>();
            brokerDetails.put(SupportedParameters.CREATE_SERVICE_BROKER, true);
            brokerDetails.put(SupportedParameters.SERVICE_BROKER_NAME, brokerName);
            brokerDetails.put(SupportedParameters.SERVICE_BROKER_URL, brokerUrl);
            brokerDetails.put(SupportedParameters.SERVICE_BROKER_USERNAME, brokerUsername);
            brokerDetails.put(SupportedParameters.SERVICE_BROKER_PASSWORD, brokerPassword);
            return brokerDetails;
        }
    }

    private static class SimpleBroker {
        String name;

        CloudServiceBroker toServiceBroker() {
            return (name == null) ? null
                : ImmutableCloudServiceBroker.builder()
                                             .name(name)
                                             .build();
        }
    }
}
