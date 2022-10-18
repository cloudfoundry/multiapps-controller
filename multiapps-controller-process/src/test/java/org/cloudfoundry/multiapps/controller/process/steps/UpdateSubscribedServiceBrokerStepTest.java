package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.GenericArgumentMatcher;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBroker;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;

class UpdateSubscribedServiceBrokerStepTest extends SyncFlowableStepTest<UpdateServiceBrokerSubscriberStep> {

    private static final UUID APP_GUID = UUID.randomUUID();

    public static Stream<Arguments> testExecute() {
        return Stream.of(
// @formatter:off
            // (0) With an application that matches to an existing service broker
            Arguments.of("update-subscribed-service-broker-input-00.json", null, null),
            // (1) With an application that does not-matches to an existing service broker
            Arguments.of("update-subscribed-service-broker-input-01.json", null, "Service broker with name \"test-broker\" does not exist"),
            // (2) With an application that has no password defined
            Arguments.of("update-subscribed-service-broker-input-02.json", "Missing service broker password for application \"test-application\"", null),
            // (3) With an application that broker does not match any existing broker
            Arguments.of("update-subscribed-service-broker-input-03.json", null, "Service broker with name \"test-broker-which-does-not-exist\" does not exist"),
            // (4) With an application which broker was deleted
            Arguments.of("update-subscribed-service-broker-input-04.json", null, "Service broker with name \"test-broker\" does not exist")
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(String inputLocation, String expectedExceptionMessage, String warningMessage) {
        StepInput input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, UpdateSubscribedServiceBrokerStepTest.class),
                                            StepInput.class);
        initializeParameters(input);
        if (expectedExceptionMessage != null) {
            Exception exception = assertThrows(Exception.class, () -> step.execute(execution));
            assertTrue(exception.getMessage()
                                .contains(expectedExceptionMessage));
            return;
        }
        step.execute(execution);
        validateExecution(input, warningMessage);
    }

    private void validateExecution(StepInput input, String warningMessage) {
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

    public void initializeParameters(StepInput input) {
        prepareClient(input);
        prepareContext(input);
    }

    private void prepareClient(StepInput input) {
        Mockito.when(client.getApplicationEnvironment(Mockito.eq(APP_GUID)))
            .thenReturn(input.brokerApplication.getAppEnv());
        Mockito.when(client.getServiceBroker(Mockito.anyString(), Mockito.eq(false)))
               .thenReturn(null);
        if (input.brokerApplication.brokerName.equals(input.brokerFromClient.name)) {
            Mockito.when(client.getServiceBroker(input.brokerFromClient.name, false))
                   .thenReturn(input.brokerFromClient.toServiceBroker());
        }
    }

    private void prepareContext(StepInput input) {
        context.setVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX, 0);
        List<CloudApplication> brokers = new ArrayList<>();
        brokers.add(input.brokerApplication.toCloudApplication());
        context.setVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS, brokers);
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
            return ImmutableCloudApplicationExtended.builder()
                                                    .metadata(ImmutableCloudMetadata.of(APP_GUID))
                                                    .name(name)
                                                    .env(getAppEnv())
                                                    .build();
        }

        Map<String, String> getAppEnv() {
            Map<String, Object> brokerDetails = getBrokerDetails();
            return Map.of(org.cloudfoundry.multiapps.controller.core.Constants.ENV_DEPLOY_ATTRIBUTES,
                          JsonUtil.toJson(brokerDetails));
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
