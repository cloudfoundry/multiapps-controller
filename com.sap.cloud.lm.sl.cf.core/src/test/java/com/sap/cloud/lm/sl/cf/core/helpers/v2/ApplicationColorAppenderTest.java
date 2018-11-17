package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.Arrays;

import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;

public class ApplicationColorAppenderTest extends com.sap.cloud.lm.sl.cf.core.helpers.v1.ApplicationColorAppenderTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) No application name is specified:
            {
                "mtad-01.yaml", new Expectation(Expectation.Type.RESOURCE, "mtad-01.yaml.json"),
            },
            // (1) An application name is specified:
            {
                "mtad-02.yaml", new Expectation(Expectation.Type.RESOURCE, "mtad-02.yaml.json"),
            },
// @formatter:on
        });
    }

    public ApplicationColorAppenderTest(String deploymentDescritorString, Expectation expectation) {
        super(deploymentDescritorString, expectation);
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    @Override
    protected ApplicationColorAppender getApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationColor) {
        return new ApplicationColorAppender(deployedMtaColor, applicationColor);
    }

}
