package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.google.gson.reflect.TypeToken;
import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DeleteTemporaryUrisStepTest extends AbstractStepTest<DeleteTemporaryUrisStep> {

    private static final String USER = "XSMASTER";

    private static final String ORG_NAME = "initial";
    private static final String SPACE_NAME = "initial";

    private static class StepInput {

        public String appsToDeployLocation;

        public StepInput(String appsToDeployLocation) {
            this.appsToDeployLocation = appsToDeployLocation;
        }

    }

    private static class StepOutput {

        public List<String> urisToDelete;
        public String appsToDeployLocation;

        public StepOutput(String appsToDeployLocation, List<String> urisToDelete) {
            this.urisToDelete = urisToDelete;
            this.appsToDeployLocation = appsToDeployLocation;
        }

    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) There are temporary URIs:
            {
                new StepInput("apps-to-deploy-04.json"), new StepOutput("apps-to-deploy-04.json", Arrays.asList("module-2-temp.api.cf.neo.ondemand.com", "module-3-temp.api.cf.neo.ondemand.com")),
            },
            // (1) There are no temporary URIs:
            {
                new StepInput("apps-to-deploy-07.json"), new StepOutput("apps-to-deploy-06.json", Collections.emptyList()),
            },
// @formatter:on
        });
    }

    private List<CloudApplicationExtended> expectedAppsToDeploy;
    private List<CloudApplicationExtended> appsToDeploy;

    private StepOutput output;
    private StepInput input;
    @Mock
    private CloudFoundryClientProvider clientProvider;
    @Mock
    private CloudFoundryOperations client;

    public DeleteTemporaryUrisStepTest(StepInput input, StepOutput output) {
        this.output = output;
        this.input = input;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareClient();
    }

    private void loadParameters() throws Exception {
        expectedAppsToDeploy = JsonUtil.fromJson(TestUtil.getResourceAsString(output.appsToDeployLocation, getClass()),
            new TypeToken<List<CloudApplicationExtended>>() {
            }.getType());
        appsToDeploy = JsonUtil.fromJson(TestUtil.getResourceAsString(input.appsToDeployLocation, getClass()),
            new TypeToken<List<CloudApplicationExtended>>() {
            }.getType());
    }

    private void prepareClient() throws Exception {
        when(clientProvider.getCloudFoundryClient(anyString(), anyString(), anyString(), anyString())).thenReturn(client);
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_PORT_BASED_ROUTING, false);

        StepsUtil.setAppsToDeploy(context, appsToDeploy);

        context.setVariable(Constants.VAR_SPACE, SPACE_NAME);
        context.setVariable(Constants.VAR_ORG, ORG_NAME);

        context.setVariable(Constants.VAR_USER, USER);
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        for (String uri : output.urisToDelete) {
            Pair<String, String> hostAndDomain = UriUtil.getHostAndDomain(uri);

            verify(client).deleteRoute(hostAndDomain._1, hostAndDomain._2);
        }
        for (CloudApplicationExtended app : expectedAppsToDeploy) {
            List<String> uriss = app.getUris();
            List<String> tempUris = app.getTempUris();

            if (tempUris != null && !tempUris.isEmpty()) {
                verify(client).updateApplicationUris(app.getName(), uriss);
            }
        }
    }

    @Override
    protected DeleteTemporaryUrisStep createStep() {
        return new DeleteTemporaryUrisStep();
    }

}
