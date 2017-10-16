package com.sap.cloud.lm.sl.cf.process.steps;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.google.gson.reflect.TypeToken;
import com.sap.activiti.common.util.GsonHelper;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class ReconfigureAppEnvironmentStepTest extends AbstractStepTest<ReconfigureAppEnvironmentStep> {

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 1;
    private static final Integer MTA_MINOR_SCHEMA_VERSION = 0;

    private static class StepInput {

        public String[] expectedChangedApps;
        public String deployedAppsLocation;
        private String updatedAppsLocation;

        public StepInput(String deployedAppsLocation, String updatedAppsLocation, String[] expectedChangedApps) {
            this.updatedAppsLocation = updatedAppsLocation;
            this.deployedAppsLocation = deployedAppsLocation;
            this.expectedChangedApps = expectedChangedApps;
        }
    }

    private class ReconfigureAppEnvironmentStepStub extends ReconfigureAppEnvironmentStep {
        @Override
        protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DelegateExecution context) {
            return applicationsCloudModelBuilder;
        }
    }

    @Override
    protected ReconfigureAppEnvironmentStep createStep() {
        return new ReconfigureAppEnvironmentStepStub();
    }

    @Parameters
    public static Iterable<Object> getParameters() {
        return Arrays.asList(new Object[] {
            //@formatter:off
            new StepInput("apps-to-deploy-08.json", "apps-to-deploy-09.json",new String[]{"module-1","module-2"}),
            //@formatter:on
        });
    }

    private StepInput input;

    @Mock
    private ApplicationsCloudModelBuilder applicationsCloudModelBuilder;
    private List<CloudApplicationExtended> updatedAppsData;

    public ReconfigureAppEnvironmentStepTest(StepInput input) {
        this.input = input;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
    }

    private void loadParameters() throws Exception {
        String appsToDeployString = TestUtil.getResourceAsString(input.deployedAppsLocation, getClass());
        List<CloudApplicationExtended> deployedAppsdata = JsonUtil.fromJson(appsToDeployString,
            new TypeToken<List<CloudApplicationExtended>>() {
        }.getType());
        StepsUtil.setAppsToDeploy(context, deployedAppsdata);

        String updatedAppsLocation = TestUtil.getResourceAsString(input.updatedAppsLocation, getClass());
        updatedAppsData = JsonUtil.fromJson(updatedAppsLocation,
            new TypeToken<List<CloudApplicationExtended>>() {
        }.getType());
        when(applicationsCloudModelBuilder.build(any(), any(), any())).thenReturn(updatedAppsData);
    }

    private void prepareContext() throws Exception {
        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);
        context.setVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION, MTA_MINOR_SCHEMA_VERSION);
        byte[] serviceKeysToInjectByteArray = GsonHelper.getAsBinaryJson(new HashMap<>());
        context.setVariable(Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT, serviceKeysToInjectByteArray);

        StepsUtil.setMtaModules(context, Collections.emptySet());
        StepsUtil.setMtaArchiveModules(context, Collections.emptySet());
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);
        assertStepFinishedSuccessfully();
        assertThat(StepsUtil.getAppsToRestart(context), hasItems(input.expectedChangedApps));
        for (String appName : input.expectedChangedApps) {
            CloudApplicationExtended expectedApp = updatedAppsData.stream().filter(
                (app) -> appName.equals(app.getName())).findFirst().get();
            verify(client).updateApplicationEnv(eq(appName), eq(expectedApp.getEnv()));
        }
        verifyNoMoreInteractions(client);
        TestUtil.test(() -> StepsUtil.getAppsToDeploy(context), "R:" + input.updatedAppsLocation, getClass());
    }
}
