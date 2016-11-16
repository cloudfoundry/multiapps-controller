package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.reflect.TypeToken;
import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class SwapUrisStepTest extends AbstractStepTest<SwapUrisStep> {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) There are temporary URIs:
            {
                "apps-to-deploy-04.json", "R:apps-to-deploy-05.json",
            },
            // (1) There are no temporary URIs:
            {
                "apps-to-deploy-06.json", "R:apps-to-deploy-07.json",
            },
// @formatter:on
        });
    }

    private List<CloudApplicationExtended> appsToDeploy;

    private String appsToDeployLocation;
    private String expected;

    public SwapUrisStepTest(String appsToDeployLocation, String expected) {
        this.appsToDeployLocation = appsToDeployLocation;
        this.expected = expected;
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

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        TestUtil.test(() -> StepsUtil.getAppsToDeploy(context), expected, getClass());
    }

    @Override
    protected SwapUrisStep createStep() {
        return new AssignTemporaryUrisStep();
    }

}
