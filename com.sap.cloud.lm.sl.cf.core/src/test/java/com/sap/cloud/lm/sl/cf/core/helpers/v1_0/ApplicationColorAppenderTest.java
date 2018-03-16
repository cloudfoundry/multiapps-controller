package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class ApplicationColorAppenderTest {

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

    private String deploymentDescriptorString;
    private String expected;

    public ApplicationColorAppenderTest(String deploymentDescritorString, String expected) {
        this.deploymentDescriptorString = deploymentDescritorString;
        this.expected = expected;
    }

    @Test
    public void testPrepare() throws Exception {
        DeploymentDescriptor descriptor = getDescriptorParser()
            .parseDeploymentDescriptorYaml(TestUtil.getResourceAsString(deploymentDescriptorString, getClass()));

        TestUtil.test(() -> {

            descriptor.accept(getApplicationColorAppender(ApplicationColor.BLUE, ApplicationColor.GREEN));
            return descriptor;

        }, expected, getClass());
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    protected ApplicationColorAppender getApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationColor) {
        return new ApplicationColorAppender(deployedMtaColor, applicationColor);
    }

}
