package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class ApplicationColorAppenderTest {

    private final Tester tester = Tester.forClass(getClass());

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) No application name is specified:
            {
                "mtad-01.yaml", new Expectation(Expectation.Type.JSON, "mtad-01.yaml.json"),
            },
            // (1) An application name is specified:
            {
                "mtad-02.yaml", new Expectation(Expectation.Type.JSON, "mtad-02.yaml.json"),
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
        DeploymentDescriptor descriptor = getDescriptorParser().parseDeploymentDescriptorYaml(TestUtil.getResourceAsString(deploymentDescriptorString,
                                                                                                                           getClass()));

        tester.test(() -> {

            descriptor.accept(getApplicationColorAppender(ApplicationColor.BLUE, ApplicationColor.GREEN));
            return descriptor;

        }, expectation);
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    protected ApplicationColorAppender getApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationColor) {
        return new ApplicationColorAppender(deployedMtaColor, applicationColor);
    }

}
