package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationSubscription.ResourceDto;
import org.cloudfoundry.multiapps.controller.core.persistence.query.ConfigurationSubscriptionQuery;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.core.util.MockBuilder;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class CreateSubscriptionsStepTest extends SyncFlowableStepTest<CreateSubscriptionsStep> {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0)
            {
                "create-subscriptions-step-input-00.json", null,
            },
            // (1)
            {
                "create-subscriptions-step-input-01.json", null,
            },
            // (2) A NPE should not be thrown when any of the subscription's components is null:
            {
                "create-subscriptions-step-input-02.json", null,
            },
// @formatter:on
        });
    }

    @Mock
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationSubscriptionQuery configurationSubscriptionQuery;

    private final String inputLocation;
    private final String expectedExceptionMessage;
    private StepInput input;

    public CreateSubscriptionsStepTest(String inputLocation, String expectedExceptionMessage) {
        this.inputLocation = inputLocation;
        this.expectedExceptionMessage = expectedExceptionMessage;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareSubscriptionService();
    }

    private void loadParameters() {
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
        }
        input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, getClass()), StepInput.class);
    }

    private void prepareContext() {
        List<ConfigurationSubscription> subscriptions = new ArrayList<>();
        subscriptions.addAll(input.subscriptionsToCreate);
        subscriptions.addAll(input.subscriptionsToUpdate);
        context.setVariable(Variables.SUBSCRIPTIONS_TO_CREATE, subscriptions);
    }

    private void prepareSubscriptionService() {
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

    @Test
    public void testExecute() throws Exception {
        step.execute(execution);

        assertStepFinishedSuccessfully();

        StepOutput output = captureStepOutput();

        assertEquals(JsonUtil.toJson(input.subscriptionsToCreate, true), JsonUtil.toJson(output.createdSubscriptions, true));
        assertEquals(JsonUtil.toJson(input.oldSubscriptionsToBeUpdated, true), JsonUtil.toJson(output.oldSubscriptionsToBeUpdated, true));
        assertEquals(JsonUtil.toJson(input.subscriptionsToUpdate, true), JsonUtil.toJson(output.updatedSubscriptions, true));
    }

    private StepOutput captureStepOutput() {
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
