package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationSubscriptionQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.process.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RemoveNewApplicationsSuffixStepTest extends SyncFlowableStepTest<RemoveNewApplicationsSuffixStep> {

    @Mock
    private ConfigurationSubscriptionService subscriptionService;

    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationSubscriptionQuery query;

    @BeforeEach
    public void setUp() {
        context.setVariable(Constants.PARAM_KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY, true);
        Mockito.when(query.list())
               .thenReturn(Collections.emptyList());
        Mockito.when(subscriptionService.createQuery())
               .thenReturn(query);
    }

    @Test
    public void testExecuteWithNoAppsToDeploy() {
        StepsUtil.setAppsToDeploy(context, Collections.emptyList());

        step.execute(context);
        assertStepFinishedSuccessfully();

        Mockito.verify(client, Mockito.never())
               .rename(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testExecuteRenamesApps() {
        List<String> apps = Arrays.asList("a-idle", "b-idle");
        StepsUtil.setAppsToDeploy(context, apps);

        step.execute(context);
        assertStepFinishedSuccessfully();

        for (String app : apps) {
            Mockito.verify(client)
                   .rename(app, removeAppNameSuffix(app));
        }
    }

    @Test
    public void testUpdatingOfConfigurationSubscriptions() {
        Mockito.when(query.list())
               .thenReturn(Collections.singletonList(new ConfigurationSubscription(0, "", "", "a-idle", null, null, null)));
        Mockito.when(subscriptionService.createQuery())
               .thenReturn(query);

        StepsUtil.setAppsToDeploy(context, Collections.singletonList("a-idle"));

        step.execute(context);
        assertStepFinishedSuccessfully();

        Mockito.verify(subscriptionService)
               .update(0L, new ConfigurationSubscription(0, "", "", "a", null, null, null));
    }

    private static String removeAppNameSuffix(String appName) {
        return appName.substring(0, appName.lastIndexOf('-'));
    }

    @Override
    protected RemoveNewApplicationsSuffixStep createStep() {
        return new RemoveNewApplicationsSuffixStep();
    }

}
