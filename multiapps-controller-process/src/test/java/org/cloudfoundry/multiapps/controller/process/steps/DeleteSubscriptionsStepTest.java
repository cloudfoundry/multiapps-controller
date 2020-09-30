package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationSubscriptionQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.ConfigurationSubscriptionQueryImpl;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;

class DeleteSubscriptionsStepTest extends SyncFlowableStepTest<DeleteSubscriptionsStep> {

    @Mock
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationSubscriptionQuery configurationSubscriptionQuery;

    public static Stream<Arguments> testExecute() {
        return Stream.of(
// @formatter:off
            // (0)
            Arguments.of(new StepInput(List.of(1L, 2L, 3L),  List.of(1L, 2L, 3L)), null),
            // (1)
            Arguments.of(new StepInput(List.of(1L, 2L, 3L),  List.of(1L, 2L, 3L, 4L, 5L, 6L)), null),
            // (2)
            Arguments.of(new StepInput(Collections.emptyList(),  List.of(1L, 2L, 3L)), null),
            // (3) A NotFoundException should not be thrown if the subscriptions were already deleted:
            Arguments.of(new StepInput(List.of(1L, 2L, 3L), Collections.emptyList()), null)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(StepInput input, String expectedExceptionMessage) {
        initializeParameters(input);
        if (expectedExceptionMessage != null) {
            Exception exception = assertThrows(Exception.class, () -> step.execute(execution));
            assertEquals(expectedExceptionMessage, exception.getMessage());
            return;
        }
        initSubscriptionQueries(input);

        step.execute(execution);

        assertStepFinishedSuccessfully();

        for (Long subscription : input.existingSubscriptions) {
            if (input.subscriptionsToDelete.contains(subscription)) {
                verify(configurationSubscriptionQuery.id(subscription)).delete();
            } else {
                verify(configurationSubscriptionQuery.id(subscription), never()).delete();
            }
        }
    }

    public void initializeParameters(StepInput input) {
        prepareContext(input);
        prepareConfigurationSubscriptionService(input);
    }

    private void prepareContext(StepInput input) {
        context.setVariable(Variables.SUBSCRIPTIONS_TO_DELETE, asSubscriptions(input.subscriptionsToDelete));
    }

    private List<ConfigurationSubscription> asSubscriptions(List<Long> subscriptionsToDelete) {
        return subscriptionsToDelete.stream()
                                    .map(this::asSubscription)
                                    .collect(Collectors.toList());
    }

    private ConfigurationSubscription asSubscription(Long subscriptionId) {
        return new ConfigurationSubscription(subscriptionId, null, null, null, null, null, null, null, null);
    }

    private void prepareConfigurationSubscriptionService(StepInput input) {
        List<Long> nonExistingSubscriptions = new ArrayList<>(input.subscriptionsToDelete);
        nonExistingSubscriptions.removeAll(input.existingSubscriptions);
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        for (Long subscription : nonExistingSubscriptions) {
            ConfigurationSubscriptionQuery nonExistingSubscriptionQueryMock = Mockito.mock(ConfigurationSubscriptionQuery.class);
            doReturn(nonExistingSubscriptionQueryMock).when(configurationSubscriptionQuery)
                                                      .id(subscription);
        }
    }

    private void initSubscriptionQueries(StepInput input) {
        for (Long subscription : input.existingSubscriptions) {
            ConfigurationSubscriptionQuery mock = Mockito.mock(ConfigurationSubscriptionQueryImpl.class);
            doReturn(mock).when(configurationSubscriptionQuery)
                          .id(subscription);
        }
    }

    private static class StepInput {

        public final List<Long> subscriptionsToDelete;
        public final List<Long> existingSubscriptions;

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
