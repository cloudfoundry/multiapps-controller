package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class CheckAppStepTest extends AbstractStepTest<CheckAppStep> {

    private final StepInput stepInput;

    private CloudApplication expectedResult;
    private CloudApplication application;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Application does not exist:
            {
                "check-app-step-input-1.json",
            },
            // (1) Application exists:
            {
                "check-app-step-input-2.json",
            },
            // (2) Client throws an exception with a 'not found' status code:
            {
                "check-app-step-input-3.json",
            },
// @formatter:on
        });
    }

    public CheckAppStepTest(String stepInput) throws IOException, ParsingException {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput, CheckAppStepTest.class), StepInput.class);
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareClient();
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        CloudApplication actualResult = StepsUtil.getExistingApp(context);
        assertEquals(JsonUtil.toJson(expectedResult, true), JsonUtil.toJson(actualResult, true));
    }

    private void loadParameters() {
        application = stepInput.applications.get(stepInput.applications.size() - 1);
        expectedResult = stepInput.isExistingApplication && !stepInput.throwException ? application : null;
    }

    private void prepareContext() {
        StepsUtil.setAppsToDeploy(context, stepInput.applications);
        StepsTestUtil.mockApplicationsToDeploy(stepInput.applications, context);
        context.setVariable(Constants.VAR_APPS_INDEX, stepInput.applicationIndex);
    }

    private void prepareClient() {
        if (!stepInput.throwException) {
            Mockito.when(client.getApplication(application.getName())).thenReturn(expectedResult);
        } else {
            Mockito.when(client.getApplication(application.getName())).thenThrow(new CloudFoundryException(HttpStatus.NOT_FOUND));
        }
    }

    private static class StepInput {
        boolean isExistingApplication;
        int applicationIndex;
        List<CloudApplicationExtended> applications;
        boolean throwException;
    }

    @Override
    protected CheckAppStep createStep() {
        return new CheckAppStep();
    }

}
