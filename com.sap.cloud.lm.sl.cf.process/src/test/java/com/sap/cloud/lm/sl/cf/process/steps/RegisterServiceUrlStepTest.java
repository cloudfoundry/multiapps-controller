package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class RegisterServiceUrlStepTest extends SyncFlowableStepTest<RegisterServiceUrlStep> {

    private final String expectedExceptionMessage;
    private final String inputLocation;
    private final String expectedOutputLocation;

    private StepInput input;
    private StepOutput expectedOutput;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0)
            {
                "register-service-urls-step-input-01.json", "register-service-urls-step-output-01.json", null,
            },
            // (1)
            {
                "register-service-urls-step-input-02.json", null, "No service URL is specified for service \"test-service-1\" and application \"test-app-2\"",
            }
// @formatter:on
        });
    }

    public RegisterServiceUrlStepTest(String inputLocation, String expectedOutputLocation, String expectedExceptionMessage) {
        this.expectedOutputLocation = expectedOutputLocation;
        this.expectedExceptionMessage = expectedExceptionMessage;
        this.inputLocation = inputLocation;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        StepOutput actualOutput = captureStepOutput();
        assertEquals(JsonUtil.toJson(expectedOutput, true), JsonUtil.toJson(actualOutput, true));

        ServiceUrl serviceUrl = expectedOutput.serviceUrlToRegister;
        Mockito.verify(client)
            .registerServiceURL(serviceUrl.getServiceName(), serviceUrl.getUrl());
    }

    private void prepareContext() {
        StepsUtil.setApp(context, input.application.toCloudApplicationExtended());
    }

    private void loadParameters() throws Exception {
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
            expectedException.expect(SLException.class);
        } else {
            expectedOutput = JsonUtil.fromJson(TestUtil.getResourceAsString(expectedOutputLocation, getClass()), StepOutput.class);
        }
        input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, getClass()), StepInput.class);
    }

    private StepOutput captureStepOutput() {
        StepOutput output = new StepOutput();

        output.serviceUrlToRegister = StepsUtil.getServiceUrlToRegister(context);

        return output;
    }

    private static class StepInput {
        SimpleApplication application;
    }

    private static class StepOutput {
        ServiceUrl serviceUrlToRegister;
    }

    private static class SimpleApplication {

        String name;
        Map<String, Object> attributes;

        CloudApplicationExtended toCloudApplicationExtended() {
            CloudApplicationExtended app = new CloudApplicationExtended(null, name);
            app.setEnv(MapUtil.asMap("DEPLOY_ATTRIBUTES", JsonUtil.toJson(attributes)));
            return app;
        }

    }

    @Override
    protected RegisterServiceUrlStep createStep() {
        return new RegisterServiceUrlStep();
    }

}
