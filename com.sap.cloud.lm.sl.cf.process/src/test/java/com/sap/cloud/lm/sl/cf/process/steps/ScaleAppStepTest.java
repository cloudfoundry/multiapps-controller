package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@RunWith(Parameterized.class)
public class ScaleAppStepTest extends SyncFlowableStepTest<ScaleAppStep> {

    private final SimpleApplication application;
    private final SimpleApplication existingApplication;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            {
                new SimpleApplication("test-app-1", 2), new SimpleApplication("test-app-1", 3),
            },
            {
                new SimpleApplication("test-app-1", 2), new SimpleApplication("test-app-1", 2),
            },
            {
                new SimpleApplication("test-app-1", 2), null,
            },
            // @formatter:on
        });
    }

    public ScaleAppStepTest(SimpleApplication application, SimpleApplication existingApplication) {
        this.application = application;
        this.existingApplication = existingApplication;
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        validateUpdatedApplications();
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_MODULES_INDEX, 0);
        context.setVariable("appToDeploy", JsonUtil.toJson(application.toCloudApplication()));
        StepsUtil.setAppsToDeploy(context, toCloudApplication());
        StepsUtil.setExistingApp(context, (existingApplication != null) ? existingApplication.toCloudApplication() : null);
    }

    List<CloudApplicationExtended> toCloudApplication() {
        return Arrays.asList(application.toCloudApplication());
    }

    private void validateUpdatedApplications() {
        if (application.instances != 0) {
            if (existingApplication == null || application.instances != existingApplication.instances) {
                Mockito.verify(client)
                    .updateApplicationInstances(application.name, application.instances);
            }
        }
    }

    static class SimpleApplication {
        String name;
        int instances;

        public SimpleApplication(String name, int instances) {
            this.name = name;
            this.instances = instances;
        }

        CloudApplicationExtended toCloudApplication() {
            CloudApplicationExtended app = new CloudApplicationExtended(null, name);
            app.setModuleName(name);
            app.setInstances(instances);
            return app;
        }
    }

    @Override
    protected ScaleAppStep createStep() {
        return new ScaleAppStep();
    }

}
