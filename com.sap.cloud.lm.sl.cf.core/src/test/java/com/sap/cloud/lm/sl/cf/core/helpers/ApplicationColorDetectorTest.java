package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class ApplicationColorDetectorTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (1) All applications are of one color (G):
            {
                "deployed-mta-02.json", new String[] { "\"GREEN\"", "\"GREEN\"", },
            },
            // (0) All applications are of one color (B):
            {
                "deployed-mta-01.json", new String[] { "\"BLUE\"", "\"BLUE\"", },
            },
            // (2) All applications have no color:
            {
                "deployed-mta-03.json", new String[] { "\"BLUE\"", "\"BLUE\"", },
            },
            // (3) Some applications are blue, but some are green:
            {
                "deployed-mta-04.json", new String[] { "E:There are both blue and green applications already deployed for MTA \"com.sap.sample.mta.consumer\"", "\"GREEN\"", },
            },
            // (4) There are no deployed modules:
            {
                "deployed-mta-05.json", new String[] { "null", "null", },
            },
            // (5) Some applications are blue, but some are green (same module):
            {
                "deployed-mta-06.json", new String[] { "E:There are both blue and green applications already deployed for MTA \"com.sap.sample.mta.consumer\"", "\"GREEN\"", },
            },
            // (6) There is no deployed MTA:
            {
                null, new String[] { "null", "null", },
            },
// @formatter:on
        });
    }

    private DeployedMta deployedMta;

    private final String deployedMtaJsonLocation;
    private final String[] expected;

    public ApplicationColorDetectorTest(String deployedMtaJsonLocation, String[] expected) {
        this.deployedMtaJsonLocation = deployedMtaJsonLocation;
        this.expected = expected;
    }

    @Before
    public void setUp() throws Exception {
        if (deployedMtaJsonLocation != null) {
            deployedMta = JsonUtil.fromJson(TestUtil.getResourceAsString(deployedMtaJsonLocation, ApplicationColorDetectorTest.class),
                DeployedMta.class);
        }
    }

    @Test
    public void testDetectSingularDeployedApplicationColor() {
        TestUtil.test(() -> new ApplicationColorDetector().detectSingularDeployedApplicationColor(deployedMta), expected[0], getClass());
    }

    @Test
    public void testDetectFirstDeployedApplicationColor() {
        TestUtil.test(() -> new ApplicationColorDetector().detectFirstDeployedApplicationColor(deployedMta), expected[1], getClass());
    }

}
