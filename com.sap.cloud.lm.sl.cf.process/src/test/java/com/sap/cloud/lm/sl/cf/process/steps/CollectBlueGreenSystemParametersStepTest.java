package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;

import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;

public class CollectBlueGreenSystemParametersStepTest extends CollectSystemParametersStepTest {

    public CollectBlueGreenSystemParametersStepTest(StepInput input, StepOutput output) {
        super(input, output);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Should not use namespaces for applications and services:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , false, false, "XSMASTER", "initial", "initial", 2, null, PlatformType.XS2, false, true), 
                new StepOutput(new Expectation(Expectation.Type.RESOURCE, "allocated-ports-3.json"), new Expectation(Expectation.Type.RESOURCE, "system-parameters-08.json"), null),
            },
            // (1) Should use namespaces for applications and services:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , true , true , "XSMASTER", "initial", "initial", 2, null, PlatformType.XS2, false, true), 
                new StepOutput(new Expectation(Expectation.Type.RESOURCE, "allocated-ports-3.json"), new Expectation(Expectation.Type.RESOURCE, "system-parameters-09.json"), null),
            },
            // (2) There are deployed MTAs:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , true , true , "XSMASTER", "initial", "initial", 2, "deployed-mta-01.json", PlatformType.CF, false, true), 
                new StepOutput(new Expectation(Expectation.Type.RESOURCE, "allocated-ports-3.json"), new Expectation(Expectation.Type.RESOURCE, "system-parameters-05.json"), null),
            },
            // (3) Host based routing:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", false, true , true , "XSMASTER", "initial", "initial", 2, null, PlatformType.XS2, false, true), 
                new StepOutput(null, new Expectation(Expectation.Type.RESOURCE, "system-parameters-10.json"), null),            },
            // (4) Should not use namespaces for applications and services (XS placeholders are supported):
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , false, false, "XSMASTER", "initial", "initial", 2, null, PlatformType.XS2, true, true), 
                new StepOutput(new Expectation(Expectation.Type.RESOURCE, "allocated-ports-3.json"), new Expectation(Expectation.Type.RESOURCE, "system-parameters-11.json"), null),
            },
            // (5) The version of the MTA is lower than the version of the previously deployed MTA:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , true , true , "XSMASTER", "initial", "initial", 2, "deployed-mta-02.json", PlatformType.CF, false, true), 
                new StepOutput(new Expectation(Expectation.Type.RESOURCE, "allocated-ports-1.json"), new Expectation(Expectation.Type.RESOURCE, "system-parameters-05.json"), Messages.HIGHER_VERSION_ALREADY_DEPLOYED),
            },
// @formatter:on
        });
    }

    @Before
    public void prepareContext() {
        context.setVariable(Constants.PARAM_NO_CONFIRM, false);
    }

    @Override
    protected CollectSystemParametersStep createStep() {
        return new CollectBlueGreenSystemParametersStep();
    }

}
