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
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;

@RunWith(Parameterized.class)
public class ApplicationColorDetectorTest {

    private final String deployedMtaJsonLocation;
    private final Expectation[] expectations;
    private DeployedMta deployedMta;
    public ApplicationColorDetectorTest(String deployedMtaJsonLocation, Expectation[] expectations) {
        this.deployedMtaJsonLocation = deployedMtaJsonLocation;
        this.expectations = expectations;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (1) All applications are of one color (G):
            {
                "deployed-mta-02.json", new Expectation[] { new Expectation("GREEN"), new Expectation("GREEN"), },
            },
            // (0) All applications are of one color (B):
            {
                "deployed-mta-01.json", new Expectation[] { new Expectation("BLUE"), new Expectation("BLUE"), },
            },
            // (2) All applications have no color:
            {
                "deployed-mta-03.json", new Expectation[] { new Expectation("BLUE"), new Expectation("BLUE"), },
            },
            // (3) Some applications are blue, but some are green:
            {
                "deployed-mta-04.json", new Expectation[] { new Expectation(Expectation.Type.EXCEPTION, "There are both blue and green applications already deployed for MTA \"com.sap.sample.mta.consumer\""), new Expectation("GREEN"), },
            },
            // (4) There are no deployed modules:
            {
                "deployed-mta-05.json", new Expectation[] { new Expectation(null), new Expectation(null), },
            },
            // (5) Some applications are blue, but some are green (same module):
            {
                "deployed-mta-06.json", new Expectation[] { new Expectation(Expectation.Type.EXCEPTION, "There are both blue and green applications already deployed for MTA \"com.sap.sample.mta.consumer\""), new Expectation("GREEN"), },
            },
            // (6) There is no deployed MTA:
            {
                null, new Expectation[] { new Expectation(null), new Expectation(null), },
            },
// @formatter:on
        });
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
        TestUtil.test(() -> new ApplicationColorDetector().detectSingularDeployedApplicationColor(deployedMta), expectations[0],
                      getClass());
    }

    @Test
    public void testDetectFirstDeployedApplicationColor() {
        TestUtil.test(() -> new ApplicationColorDetector().detectFirstDeployedApplicationColor(deployedMta), expectations[1], getClass());
    }

}
