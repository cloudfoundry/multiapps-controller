package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;

@RunWith(Parameterized.class)
public class SetAppsUrisStepTest extends SyncFlowableStepTest<SetAppsUrisStep> {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) There are temporary URIs:
            {
                "apps-to-deploy-04.json", new Expectation(Expectation.Type.RESOURCE, "apps-to-deploy-10.json"),
            },
            // (1) There are no temporary URIs:
            {
                "apps-to-deploy-06.json", new Expectation(Expectation.Type.RESOURCE, "apps-to-deploy-07.json"),
            },
// @formatter:on
        });
    }

    private List<CloudApplicationExtended> appsToDeploy;

    private String appsToDeployLocation;
    private Expectation expectation;

    public SetAppsUrisStepTest(String appsToDeployLocation, Expectation expectation) {
        this.appsToDeployLocation = appsToDeployLocation;
        this.expectation = expectation;
    }

    @Before
    public void setUp() throws Exception {
        String appsToDeployString = TestUtil.getResourceAsString(appsToDeployLocation, getClass());
        appsToDeploy = JsonUtil.fromJson(appsToDeployString, new TypeToken<List<CloudApplicationExtended>>() {
        }.getType());
        StepsUtil.setAppsToDeploy(context, appsToDeploy);
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();
        TestUtil.test(() -> StepsUtil.getAppsToDeploy(context), expectation, getClass());
    }

    @Override
    protected SetAppsUrisStep createStep() {
        return new AssignIdleUrisStep();
    }

}
