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

        public String appToDeployLocation;

        public StepInput(String appToDeployLocation) {
            this.appToDeployLocation = appToDeployLocation;
        }

    }

    private static class StepOutput {

        public List<String> urisToDelete;
        public String appToDeployLocation;

        public StepOutput(String appToDeployLocation, List<String> urisToDelete) {
            this.urisToDelete = urisToDelete;
            this.appToDeployLocation = appToDeployLocation;
        }

    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) There are idle URIs:
            {
                new StepInput("app-to-deploy-1.json"), new StepOutput("app-to-deploy-1.json", Arrays.asList("module-1-idle.domain.com")),
            },
            // (1) There are no idle URIs:
            {
                new StepInput("app-to-deploy-2.json"), new StepOutput("app-to-deploy-2.json", Collections.emptyList()),
            },
            // (2) There are idle TCP URIs:
            {
                new StepInput("app-to-deploy-3.json"), new StepOutput("app-to-deploy-3.json", Arrays.asList("tcp://test.domain.com:51052")),
            },
// @formatter:on
        });
    }

    private CloudApplicationExtended expectedAppToDeploy;
    private CloudApplicationExtended appToDeploy;

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
    }

    private void loadParameters() throws Exception {
        expectedAppToDeploy = JsonUtil.fromJson(TestUtil.getResourceAsString(output.appToDeployLocation, getClass()),
            new TypeToken<CloudApplicationExtended>() {
            }.getType());
        appToDeploy = JsonUtil.fromJson(TestUtil.getResourceAsString(input.appToDeployLocation, getClass()),
            new TypeToken<CloudApplicationExtended>() {
            }.getType());
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_PORT_BASED_ROUTING, false);
        
        if (!output.urisToDelete.isEmpty()) {
            StepsUtil.setDeleteIdleUris(context, true);
        }
        
        CloudApplicationExtended existingApp = new CloudApplicationExtended(null, expectedAppToDeploy.getName());
        existingApp.setUris(output.urisToDelete);
        StepsUtil.setExistingApp(context, existingApp);

        StepsUtil.setApp(context, appToDeploy);
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
