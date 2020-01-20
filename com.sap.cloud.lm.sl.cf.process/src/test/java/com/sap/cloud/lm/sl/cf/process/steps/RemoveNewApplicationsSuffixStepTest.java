package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationSubscriptionQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.process.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RemoveNewApplicationsSuffixStepTest extends SyncFlowableStepTest<RemoveNewApplicationsSuffixStep> {

    @Mock
    private ConfigurationSubscriptionService subscriptionService;

    @BeforeEach
    public void setUp() {
        context.setVariable(Constants.PARAM_KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY, true);
        context.setVariable(Constants.PARAM_MTA_ID, "");
        ConfigurationSubscriptionQuery query = Mockito.mock(ConfigurationSubscriptionQuery.class);
        Mockito.when(query.mtaId(""))
               .thenReturn(query);
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
        List<String> apps = Arrays.asList("a-new", "b-new");
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
        ConfigurationSubscriptionQuery query = Mockito.mock(ConfigurationSubscriptionQuery.class);
        Mockito.when(query.mtaId(""))
               .thenReturn(query);
        Mockito.when(query.list())
               .thenReturn(Collections.singletonList(new ConfigurationSubscription(0, "", "", "a-new", null, null, null)));
        Mockito.when(subscriptionService.createQuery())
               .thenReturn(query);

        StepsUtil.setAppsToDeploy(context, Collections.singletonList("a-new"));

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
