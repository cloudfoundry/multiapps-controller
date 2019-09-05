package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription.ResourceDto;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationSubscriptionQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.core.util.MockBuilder;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class CreateSubscriptionsStepTest extends SyncFlowableStepTest<CreateSubscriptionsStep> {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final long DUMMY_ID = 9L;

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
            // (2) A NPE should not be thrown when any of the subscripton's components is null:
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

    private void loadParameters() throws Exception {
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
        }
        input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, getClass()), StepInput.class);
    }

    private void prepareContext() {
        List<ConfigurationSubscription> subscriptions = new ArrayList<>();
        subscriptions.addAll(input.subscriptionsToCreate);
        subscriptions.addAll(input.subscriptionsToUpdate);
        StepsUtil.setSubscriptionsToCreate(context, subscriptions);
    }

    private void prepareSubscriptionService() throws Exception {
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        doReturn(null).when(configurationSubscriptionQuery)
                      .singleResult();
        for (int i = 0; i < input.subscriptionsToUpdate.size(); i++) {
            ConfigurationSubscription subscription = input.subscriptionsToUpdate.get(i);
            ResourceDto resourceDto = subscription.getResourceDto();
            if (resourceDto == null) {
                continue;
            }
            ConfigurationSubscriptionQuery queryMock = new MockBuilder<>(configurationSubscriptionQuery).on(query -> query.appName(subscription.getAppName()))
                                                                                                        .on(query -> query.spaceId(subscription.getSpaceId()))
                                                                                                        .on(query -> query.resourceName(resourceDto.getName()))
                                                                                                        .on(query -> query.mtaId(subscription.getMtaId()))
                                                                                                        .build();
            doReturn(setId(subscription, DUMMY_ID)).when(queryMock)
                                                   .singleResult();
        }
    }

    private ConfigurationSubscription setId(ConfigurationSubscription subscription, long id) {
        return new ConfigurationSubscription(id,
                                             subscription.getMtaId(),
                                             subscription.getSpaceId(),
                                             subscription.getAppName(),
                                             subscription.getFilter(),
                                             subscription.getModuleDto(),
                                             subscription.getResourceDto());
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        StepOutput output = captureStepOutput();

        assertEquals(JsonUtil.toJson(output.createdSubscriptions, true), JsonUtil.toJson(input.subscriptionsToCreate, true));
        assertEquals(JsonUtil.toJson(output.updatedSubscriptions, true), JsonUtil.toJson(input.subscriptionsToUpdate, true));
    }

    private StepOutput captureStepOutput() throws Exception {
        ArgumentCaptor<ConfigurationSubscription> argumentCaptor;

        StepOutput output = new StepOutput();

        argumentCaptor = ArgumentCaptor.forClass(ConfigurationSubscription.class);
        Mockito.verify(configurationSubscriptionService, times(input.subscriptionsToUpdate.size()))
               .update(eq(DUMMY_ID), argumentCaptor.capture());
        output.updatedSubscriptions = argumentCaptor.getAllValues();

        argumentCaptor = ArgumentCaptor.forClass(ConfigurationSubscription.class);
        Mockito.verify(configurationSubscriptionService, times(input.subscriptionsToCreate.size()))
               .add(argumentCaptor.capture());
        output.createdSubscriptions = argumentCaptor.getAllValues();

        return output;
    }

    private static class StepInput {

        public List<ConfigurationSubscription> subscriptionsToCreate;
        public List<ConfigurationSubscription> subscriptionsToUpdate;

    }

    private static class StepOutput {

        public List<ConfigurationSubscription> createdSubscriptions;
        public List<ConfigurationSubscription> updatedSubscriptions;

    }

    @Override
    protected CreateSubscriptionsStep createStep() {
        return new CreateSubscriptionsStep();
    }

}
