package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;

@RunWith(Parameterized.class)
public class ScaleAppStepTest extends AbstractStepTest<ScaleAppStep> {

    private final SimpleApplication application;
    private final SimpleApplication existingApplication;
    private final boolean keepAppAtributes;

    private CloudFoundryOperations client = Mockito.mock(CloudFoundryOperations.class);

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            {
                new SimpleApplication("test-app-1", 2), new SimpleApplication("test-app-1", 3), false
            },
            {
                new SimpleApplication("test-app-1", 2), new SimpleApplication("test-app-1", 2), false
            },
            {
                new SimpleApplication("test-app-1", 2), new SimpleApplication("test-app-1", 2), true
            }
            ,
            {
                new SimpleApplication("test-app-1", 2), null, true
            }
            // @formatter:on
        });
    }

    public ScaleAppStepTest(SimpleApplication application, SimpleApplication existingApplication, boolean keepAttr) {
        this.application = application;
        this.existingApplication = existingApplication;
        this.keepAppAtributes = keepAttr;
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        step.clientSupplier = (context) -> client;
    }

    @Test
    public void testExecute() throws Exception {
        ExecutionStatus status = step.executeStep(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(), status.toString());

        validateUpdatedApplications();
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_APPS_INDEX, 0);
        context.setVariable(Constants.PARAM_KEEP_APP_ATTRIBUTES, keepAppAtributes);
        StepsUtil.setAppsToDeploy(context, toCloudApplication());
        StepsUtil.setExistingApp(context, (existingApplication != null) ? existingApplication.toCloudApplication() : null);
    }

    List<CloudApplicationExtended> toCloudApplication() {
        return Arrays.asList(application.toCloudApplication());
    }

    private void validateUpdatedApplications() {
        if (application.instances != 0 && !keepAppAtributes) {
            if (existingApplication == null || application.instances != existingApplication.instances) {
                Mockito.verify(client).updateApplicationInstances(application.name, application.instances);
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
