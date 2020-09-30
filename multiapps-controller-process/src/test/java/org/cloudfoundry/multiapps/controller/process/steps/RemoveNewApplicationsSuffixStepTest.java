package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationSubscriptionQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;

class RemoveNewApplicationsSuffixStepTest extends SyncFlowableStepTest<RemoveNewApplicationsSuffixStep> {

    @Mock
    private ConfigurationSubscriptionService subscriptionService;

    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationSubscriptionQuery query;

    @BeforeEach
    void setUp() {
        context.setVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY, true);
        Mockito.when(query.list())
               .thenReturn(Collections.emptyList());
        Mockito.when(subscriptionService.createQuery())
               .thenReturn(query);
    }

    @Test
    void testExecuteWithNoAppsToDeploy() {
        context.setVariable(Variables.APPS_TO_DEPLOY, Collections.emptyList());

        step.execute(execution);
        assertStepFinishedSuccessfully();

        Mockito.verify(client, Mockito.never())
               .rename(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testExecuteRenamesApps() {
        List<String> apps = List.of("a-idle", "b-idle");
        context.setVariable(Variables.APPS_TO_DEPLOY, apps);

        step.execute(execution);
        assertStepFinishedSuccessfully();

        for (String app : apps) {
            Mockito.verify(client)
                   .rename(app, removeAppNameSuffix(app));
        }
    }

    @Test
    void testUpdatingOfConfigurationSubscriptions() {
        Mockito.when(query.list())
               .thenReturn(List.of(new ConfigurationSubscription(0, "", "", "a-idle", null, null, null, null, null)));
        Mockito.when(subscriptionService.createQuery())
               .thenReturn(query);

        context.setVariable(Variables.APPS_TO_DEPLOY, List.of("a-idle"));

        step.execute(execution);
        assertStepFinishedSuccessfully();

        Mockito.verify(subscriptionService)
               .update(Mockito.any(), Mockito.eq(new ConfigurationSubscription(0, "", "", "a", null, null, null, null, null)));
    }

    private static String removeAppNameSuffix(String appName) {
        return appName.substring(0, appName.lastIndexOf('-'));
    }

    @Override
    protected RemoveNewApplicationsSuffixStep createStep() {
        return new RemoveNewApplicationsSuffixStep();
    }

}
