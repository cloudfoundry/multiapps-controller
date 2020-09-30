package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.test.MockBuilder;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription.ResourceDto;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationSubscriptionQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

class CreateSubscriptionsStepTest extends SyncFlowableStepTest<CreateSubscriptionsStep> {

    public static Stream<Arguments> testExecute() {
        return Stream.of(
// @formatter:off
            // (0)
            Arguments.of("create-subscriptions-step-input-00.json", null),
            // (1)
            Arguments.of("create-subscriptions-step-input-01.json", null),
            // (2) A NPE should not be thrown when any of the subscription's components is null:
            Arguments.of("create-subscriptions-step-input-02.json", null)
// @formatter:on
        );
    }

    @Mock
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationSubscriptionQuery configurationSubscriptionQuery;

    @ParameterizedTest
    @MethodSource
    void testExecute(String inputLocation, String expectedExceptionMessage) {
        StepInput input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, getClass()), StepInput.class);
        initializeParameters(input);
        if (expectedExceptionMessage != null) {
            Exception exception = assertThrows(Exception.class, () -> step.execute(execution));
            assertEquals(expectedExceptionMessage, exception.getMessage());
            return;
        }
        step.execute(execution);
        assertStepFinishedSuccessfully();
        StepOutput output = captureStepOutput(input);

        assertEquals(JsonUtil.toJson(input.subscriptionsToCreate, true), JsonUtil.toJson(output.createdSubscriptions, true));
        assertEquals(JsonUtil.toJson(input.oldSubscriptionsToBeUpdated, true), JsonUtil.toJson(output.oldSubscriptionsToBeUpdated, true));
        assertEquals(JsonUtil.toJson(input.subscriptionsToUpdate, true), JsonUtil.toJson(output.updatedSubscriptions, true));
    }

    private void initializeParameters(StepInput input) {
        prepareContext(input);
        prepareSubscriptionService(input);
    }

    private void prepareContext(StepInput input) {
        List<ConfigurationSubscription> subscriptions = new ArrayList<>();
        subscriptions.addAll(input.subscriptionsToCreate);
        subscriptions.addAll(input.subscriptionsToUpdate);
        context.setVariable(Variables.SUBSCRIPTIONS_TO_CREATE, subscriptions);
    }

    private void prepareSubscriptionService(StepInput input) {
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        doReturn(null).when(configurationSubscriptionQuery)
                      .singleResult();
        for (int i = 0; i < input.oldSubscriptionsToBeUpdated.size(); i++) {
            ConfigurationSubscription subscription = input.oldSubscriptionsToBeUpdated.get(i);
            ResourceDto resourceDto = subscription.getResourceDto();
            if (resourceDto == null) {
                continue;
            }
            ConfigurationSubscriptionQuery queryMock = new MockBuilder<>(configurationSubscriptionQuery).on(query -> query.appName(subscription.getAppName()))
                                                                                                        .on(query -> query.spaceId(subscription.getSpaceId()))
                                                                                                        .on(query -> query.resourceName(resourceDto.getName()))
                                                                                                        .on(query -> query.mtaId(subscription.getMtaId()))
                                                                                                        .build();
            doReturn(setId(subscription)).when(queryMock)
                                         .singleResult();
        }
    }

    private ConfigurationSubscription setId(ConfigurationSubscription subscription) {
        return new ConfigurationSubscription(subscription.getId(),
                                             subscription.getMtaId(),
                                             subscription.getSpaceId(),
                                             subscription.getAppName(),
                                             subscription.getFilter(),
                                             subscription.getModuleDto(),
                                             subscription.getResourceDto(),
                                             subscription.getModuleId(),
                                             subscription.getResourceId());
    }

    private StepOutput captureStepOutput(StepInput input) {
        ArgumentCaptor<ConfigurationSubscription> argumentCaptor;
        ArgumentCaptor<ConfigurationSubscription> oldSubscriptionsCaptor;

        StepOutput output = new StepOutput();

        argumentCaptor = ArgumentCaptor.forClass(ConfigurationSubscription.class);
        oldSubscriptionsCaptor = ArgumentCaptor.forClass(ConfigurationSubscription.class);
        Mockito.verify(configurationSubscriptionService, times(input.subscriptionsToUpdate.size()))
               .update(oldSubscriptionsCaptor.capture(), argumentCaptor.capture());
        output.oldSubscriptionsToBeUpdated = oldSubscriptionsCaptor.getAllValues();
        output.updatedSubscriptions = argumentCaptor.getAllValues();

        argumentCaptor = ArgumentCaptor.forClass(ConfigurationSubscription.class);
        Mockito.verify(configurationSubscriptionService, times(input.subscriptionsToCreate.size()))
               .add(argumentCaptor.capture());
        output.createdSubscriptions = argumentCaptor.getAllValues();

        return output;
    }

    private static class StepInput {

        public List<ConfigurationSubscription> subscriptionsToCreate;
        public List<ConfigurationSubscription> oldSubscriptionsToBeUpdated;
        public List<ConfigurationSubscription> subscriptionsToUpdate;

    }

    private static class StepOutput {

        public List<ConfigurationSubscription> createdSubscriptions;
        public List<ConfigurationSubscription> oldSubscriptionsToBeUpdated;
        public List<ConfigurationSubscription> updatedSubscriptions;

    }

    @Override
    protected CreateSubscriptionsStep createStep() {
        return new CreateSubscriptionsStep();
    }

}
