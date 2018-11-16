package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DeleteIdleRoutesStepTest extends SyncFlowableStepTest<DeleteIdleRoutesStep> {

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
            // (0) There are idle URIs:
            {
                new StepInput("apps-to-deploy-04.json"), new StepOutput("apps-to-deploy-04.json", Arrays.asList("module-2-idle.api.cf.neo.ondemand.com", "module-3-idle.api.cf.neo.ondemand.com")),
            },
            // (1) There are no idle URIs:
            {
                new StepInput("apps-to-deploy-07.json"), new StepOutput("apps-to-deploy-07.json", Collections.emptyList()),
            },
            // (2) There are idle TCP URIs:
            {
                new StepInput("apps-to-deploy-11.json"), new StepOutput("apps-to-deploy-11.json", Arrays.asList("tcp://test.domain.com:51052")),
            },
// @formatter:on
        });
    }

    private List<CloudApplicationExtended> expectedAppsToDeploy;
    private List<CloudApplicationExtended> appsToDeploy;

    private StepOutput output;
    private StepInput input;

    public DeleteIdleRoutesStepTest(StepInput input, StepOutput output) {
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

    private void prepareContext() {
        context.setVariable(Constants.VAR_PORT_BASED_ROUTING, false);

        StepsUtil.setAppsToDeploy(context, appsToDeploy);
    }

    private void prepareClient() {
        for (CloudApplicationExtended app : expectedAppsToDeploy) {
            CloudApplicationExtended existingApp = new CloudApplicationExtended(null, app.getName());
            List<String> existingUris = new ArrayList<>(app.getUris());
            existingUris.addAll(output.urisToDelete);
            existingApp.setUris(existingUris);
            when(client.getApplication(app.getName())).thenReturn(existingApp);
        }
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        if (CollectionUtils.isEmpty(output.urisToDelete)) {
            verify(client, never()).deleteRoute(anyString(), anyString(), anyString());
            return;
        }

        for (String uri : output.urisToDelete) {
            Pair<String, String> hostAndDomain = UriUtil.getHostAndDomain(uri);
            if (UriUtil.isTcpOrTcpsUri(uri)) {
                assertTrue("The host segment should be a port number", NumberUtils.isDigits(hostAndDomain._1));
            }
            verify(client, times(1)).deleteRoute(hostAndDomain._1, hostAndDomain._2, null);
        }
    }

    @Override
    protected DeleteIdleRoutesStep createStep() {
        return new DeleteIdleRoutesStep();
    }

}
