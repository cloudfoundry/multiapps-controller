package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationSubscriptionQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.impl.ConfigurationSubscriptionQueryImpl;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;

@RunWith(Parameterized.class)
public class DeleteSubscriptionsStepTest extends SyncFlowableStepTest<DeleteSubscriptionsStep> {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0)
            {
                new StepInput(Arrays.asList(1L, 2L, 3L),  Arrays.asList(1L, 2L, 3L)), null,
            },
            // (1)
            {
                new StepInput(Arrays.asList(1L, 2L, 3L),  Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L)), null,
            },
            // (2)
            {
               new StepInput(Collections.emptyList(),  Arrays.asList(1L, 2L, 3L)), null,
            },
            // (3) A NotFoundException should not be thrown if the subscriptions were already deleted:
            {
                new StepInput(Arrays.asList(1L, 2L, 3L), Collections.emptyList()), null,
            },
// @formatter:on
        });
    }

    @Mock
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationSubscriptionQuery configurationSubscriptionQuery;

    private final String expectedExceptionMessage;
    private final StepInput input;

    public DeleteSubscriptionsStepTest(StepInput input, String expectedExceptionMessage) {
        this.expectedExceptionMessage = expectedExceptionMessage;
        this.input = input;
    }

    @Before
    public void setUp() {
        loadParameters();
        prepareContext();
        prepareConfigurationSubscriptionService();
    }

    private void prepareContext() {
        StepsUtil.setSubscriptionsToDelete(context, asSubscriptions(input.subscriptionsToDelete));
    }

    private List<ConfigurationSubscription> asSubscriptions(List<Long> subscriptionsToDelete) {
        return subscriptionsToDelete.stream()
                                    .map(this::asSubscription)
                                    .collect(Collectors.toList());
    }

    private ConfigurationSubscription asSubscription(Long subscriptionId) {
        return new ConfigurationSubscription(subscriptionId, null, null, null, null, null, null);
    }

    private void loadParameters() {
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
        }
    }

    private void prepareConfigurationSubscriptionService() {
        List<Long> nonExistingSubscriptions = new ArrayList<>(input.subscriptionsToDelete);
        nonExistingSubscriptions.removeAll(input.existingSubscriptions);
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        for (Long subscription : nonExistingSubscriptions) {
            ConfigurationSubscriptionQuery nonExistingSubscriptionQueryMock = Mockito.mock(ConfigurationSubscriptionQuery.class);
            doReturn(nonExistingSubscriptionQueryMock).when(configurationSubscriptionQuery)
                                                      .id(subscription);
        }
    }

    @Test
    public void testExecute() {
        initSubscriptionQueries();

        step.execute(context);

        assertStepFinishedSuccessfully();

        for (Long subscription : input.existingSubscriptions) {
            if (input.subscriptionsToDelete.contains(subscription)) {
                verify(configurationSubscriptionQuery.id(subscription)).delete();
            } else {
                verify(configurationSubscriptionQuery.id(subscription), never()).delete();
            }
        }
    }

    private void initSubscriptionQueries() {
        for (Long subscription : input.existingSubscriptions) {
            ConfigurationSubscriptionQuery mock = Mockito.mock(ConfigurationSubscriptionQueryImpl.class);
            doReturn(mock).when(configurationSubscriptionQuery)
                          .id(subscription);
        }
    }

    private static class StepInput {

        public List<Long> subscriptionsToDelete;
        public List<Long> existingSubscriptions;

        public StepInput(List<Long> subscriptionsToDelete, List<Long> existingSubscriptions) {
            this.subscriptionsToDelete = subscriptionsToDelete;
            this.existingSubscriptions = existingSubscriptions;
        }

    }

    @Override
    protected DeleteSubscriptionsStep createStep() {
        return new DeleteSubscriptionsStep();
    }

}
