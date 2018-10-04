package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.Arrays;

import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.helpers.v2.ApplicationColorAppender;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;

public class ApplicationColorAppenderTest extends com.sap.cloud.lm.sl.cf.core.helpers.v1.ApplicationColorAppenderTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) No application name is specified:
            {
                "mtad-01.yaml", "R:mtad-01.yaml.json",
            },
            // (1) An application name is specified:
            {
                "mtad-02.yaml", "R:mtad-02.yaml.json",
            },
// @formatter:on
        });
    }

    public ApplicationColorAppenderTest(String deploymentDescritorString, String expected) {
        super(deploymentDescritorString, expected);
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
