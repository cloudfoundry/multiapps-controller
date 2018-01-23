package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

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
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , false, false, "XSMASTER", "initial initial", "initial", "initial", 1, 0, null, PlatformType.XS2, false), 
                new StepOutput(new TreeSet<>(Arrays.asList(1, 2, 3)), "R:system-parameters-02.json", null),
            },
            // (1) Should use namespaces for applications and services:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , true , true , "XSMASTER", "initial initial", "initial", "initial", 1, 0, null, PlatformType.XS2, false), 
                new StepOutput(new TreeSet<>(Arrays.asList(1, 2, 3)), "R:system-parameters-01.json", null),
            },
            // (2) There are deployed MTAs:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , true , true , "XSMASTER", "initial initial", "initial", "initial", 1, 0, "deployed-mta-01.json", PlatformType.CF, false), 
                new StepOutput(new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6)), "R:system-parameters-05.json", null),
            },
            // (3) Host based routing:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", false, true , true , "XSMASTER", "initial initial", "initial", "initial", 1, 0, null, PlatformType.XS2, false), 
                new StepOutput(null, "R:system-parameters-03.json", null),            },
            // (4) Should not use namespaces for applications and services (XS placeholders are supported):
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , false, false, "XSMASTER", "initial initial", "initial", "initial", 1, 0, null, PlatformType.XS2, true ), 
                new StepOutput(new TreeSet<>(Arrays.asList(1, 2, 3)), "R:system-parameters-06.json", null),
            },
            // (5) The version of the MTA is lower than the version of the previously deployed MTA:
            {
                new StepInput("node-hello-mtad.yaml", "https://localhost:30032/uaa-security", "https://deploy-service-url:51002", "localhost", true , true , true , "XSMASTER", "initial initial", "initial", "initial", 1, 0, "deployed-mta-02.json", PlatformType.CF, false), 
                new StepOutput(Collections.emptySet(), "R:system-parameters-05.json", Messages.HIGHER_VERSION_ALREADY_DEPLOYED),
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
