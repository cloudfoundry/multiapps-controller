package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

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
    public void setUp() {
        prepareContext();
    }

    @Test
    public void testExecute() {
        step.execute(execution);

        assertStepFinishedSuccessfully();

        validateUpdatedApplications();
    }

    private void prepareContext() {
        context.setVariable(Variables.MODULES_INDEX, 0);
        context.setVariable(Variables.APP_TO_PROCESS, application.toCloudApplication());
        context.setVariable(Variables.APPS_TO_DEPLOY, Collections.emptyList());
        context.setVariable(Variables.EXISTING_APP, (existingApplication != null) ? existingApplication.toCloudApplication() : null);
    }

    List<CloudApplicationExtended> toCloudApplication() {
        return Collections.singletonList(application.toCloudApplication());
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
        final String name;
        final int instances;

        public SimpleApplication(String name, int instances) {
            this.name = name;
            this.instances = instances;
        }

        CloudApplicationExtended toCloudApplication() {
            return ImmutableCloudApplicationExtended.builder()
                                                    .name(name)
                                                    .moduleName(name)
                                                    .instances(instances)
                                                    .build();
        }
    }

    @Override
    protected ScaleAppStep createStep() {
        return new ScaleAppStep();
    }

}
