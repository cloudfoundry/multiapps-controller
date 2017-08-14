package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.common.NotFoundException;

@RunWith(Parameterized.class)
public class DeleteSubscriptionsStepTest extends AbstractStepTest<DeleteSubscriptionsStep> {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0)
            {
                new StepInput(Arrays.asList(1, 2, 3),  Arrays.asList(1, 2, 3)), null,
            },
            // (1)
            {
                new StepInput(Arrays.asList(1, 2, 3),  Arrays.asList(1, 2, 3, 4, 5, 6)), null,
            },
            // (2)
            {
               new StepInput(Collections.emptyList(),  Arrays.asList(1, 2, 3)), null,
            },
            // (3) A NotFoundException should not be thrown if the subscriptions were already deleted:
            {
                new StepInput(Arrays.asList(1, 2, 3), Collections.emptyList()), null,
            },
// @formatter:on
        });
    }

    @Mock
    private ConfigurationSubscriptionDao dao;

    private final String expectedExceptionMessage;
    private final StepInput input;

    public DeleteSubscriptionsStepTest(StepInput input, String expectedExceptionMessage) {
        this.expectedExceptionMessage = expectedExceptionMessage;
        this.input = input;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareDao();
    }

    private void prepareContext() {
        StepsUtil.setSubscriptionsToDelete(context, asSubscriptions(input.subscriptionsToDelete));
    }

    private List<ConfigurationSubscription> asSubscriptions(List<Integer> subscriptionsToDelete) {
        return subscriptionsToDelete.stream().map((subscription) -> asSubscription(subscription)).collect(Collectors.toList());
    }

    private ConfigurationSubscription asSubscription(Integer subscriptionnId) {
        return new ConfigurationSubscription(subscriptionnId, null, null, null, null, null, null);
    }

    private void loadParameters() {
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
        }
    }

    private void prepareDao() throws Exception {
        List<Integer> nonExistingSubscriptions = new ArrayList<>(input.subscriptionsToDelete);
        nonExistingSubscriptions.removeAll(input.existingSubscriptions);
        for (Integer subscription : nonExistingSubscriptions) {
            when(dao.remove(subscription)).thenThrow(new NotFoundException(Messages.CONFIGURATION_SUBSCRIPTION_NOT_FOUND, subscription));
        }
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        for (Integer subscription : input.existingSubscriptions) {
            if (input.subscriptionsToDelete.contains(subscription)) {
                Mockito.verify(dao, times(1)).remove(subscription);
            } else {
                Mockito.verify(dao, times(0)).remove(subscription);
            }
        }
    }

    private static class StepInput {

        public List<Integer> subscriptionsToDelete;
        public List<Integer> existingSubscriptions;

        public StepInput(List<Integer> subscriptionsToDelete, List<Integer> existingSubscriptions) {
            this.subscriptionsToDelete = subscriptionsToDelete;
            this.existingSubscriptions = existingSubscriptions;
        }

    }

    @Override
    protected DeleteSubscriptionsStep createStep() {
        return new DeleteSubscriptionsStep();
    }

}
