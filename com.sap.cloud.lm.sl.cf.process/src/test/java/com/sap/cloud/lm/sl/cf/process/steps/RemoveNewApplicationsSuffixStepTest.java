package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.process.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RemoveNewApplicationsSuffixStepTest extends SyncFlowableStepTest<RemoveNewApplicationsSuffixStep> {

    @BeforeEach
    public void setUp() {
        context.setVariable(Constants.PARAM_KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY, true);
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

    private static String removeAppNameSuffix(String appName) {
        return appName.substring(0, appName.lastIndexOf('-'));
    }

    @Override
    protected RemoveNewApplicationsSuffixStep createStep() {
        return new RemoveNewApplicationsSuffixStep();
    }

}
