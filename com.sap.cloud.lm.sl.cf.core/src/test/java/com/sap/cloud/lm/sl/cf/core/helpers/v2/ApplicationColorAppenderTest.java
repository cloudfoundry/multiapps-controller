package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;

public class ApplicationColorAppenderTest {

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

    private String deploymentDescriptorString;
    private Expectation expectation;

    public ApplicationColorAppenderTest(String deploymentDescritorString, Expectation expectation) {
        this.deploymentDescriptorString = deploymentDescritorString;
        this.expectation = expectation;
    }

    @Test
    public void testPrepare() throws Exception {
        com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor descriptor = getDescriptorParser()
            .parseDeploymentDescriptorYaml(TestUtil.getResourceAsString(deploymentDescriptorString, getClass()));

        TestUtil.test(() -> {

            descriptor.accept(getApplicationColorAppender(ApplicationColor.BLUE, ApplicationColor.GREEN));
            return descriptor;

        }, expectation, getClass());
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    protected ApplicationColorAppender getApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationColor) {
        return new ApplicationColorAppender(deployedMtaColor, applicationColor);
    }

}
