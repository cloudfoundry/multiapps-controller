package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;

@RunWith(Parameterized.class)
public class DeployAppsStepTest extends SyncActivitiStepTest<PrepareAppsDeploymentStep> {

    private final int count;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] { { 1 }, { 2 }, { 3 }, { 4 }, { 5 } });
    }

    public DeployAppsStepTest(int count) {
        this.count = count;
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        Mockito.when(configuration.getPlatformType()).thenReturn(ApplicationConfiguration.DEFAULT_TYPE);
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        assertEquals(count, context.getVariable(Constants.VAR_APPS_COUNT));
        assertEquals(0, context.getVariable(Constants.VAR_APPS_INDEX));
    }

    private DelegateExecution prepareContext() {
        StepsUtil.setAppsToDeploy(context, getDummyApplications());
        return context;
    }

    private List<CloudApplicationExtended> getDummyApplications() {
        List<CloudApplicationExtended> applications = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            applications.add(new CloudApplicationExtended(null, "application-" + i));
        }
        return applications;
    }

    @Override
    protected PrepareAppsDeploymentStep createStep() {
        return new PrepareAppsDeploymentStep();
    }

}
