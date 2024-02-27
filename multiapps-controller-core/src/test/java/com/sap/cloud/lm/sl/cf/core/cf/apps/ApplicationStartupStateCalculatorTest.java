package com.sap.cloud.lm.sl.cf.core.cf.apps;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Enclosed.class)
public class ApplicationStartupStateCalculatorTest {

    @RunWith(Parameterized.class)
    public static class CurrentApplicationStartupStateCalculatorTest {

        private final String pathToFileContainingAppJson;
        private final ApplicationStartupState expectedState;
        private CloudApplication app;

        public CurrentApplicationStartupStateCalculatorTest(String pathToFileContainingAppJson, ApplicationStartupState expectedState) {
            this.pathToFileContainingAppJson = pathToFileContainingAppJson;
            this.expectedState = expectedState;
        }

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
                // @formatter:off
                // (0)
                {
                    "started-app.json", ApplicationStartupState.STARTED,
                },
                // (1)
                {
                    "stopped-app.json", ApplicationStartupState.STOPPED,
                },
                // (2) The number of running instances is different than the number of total instances:
                {
                    "app-in-inconsistent-state-0.json", ApplicationStartupState.INCONSISTENT,
                },
                // (3) The number of running instances is not zero when the requested state is stopped:
                {
                    "app-in-inconsistent-state-1.json", ApplicationStartupState.INCONSISTENT,
                },
                // (4) The number of running and the number of total instances is zero, but the requested state is started:
                {
                    "app-in-inconsistent-state-2.json", ApplicationStartupState.INCONSISTENT,
                },
                // (5) The number of running instances is bigger than the number of total instances:
                {
                    "app-in-inconsistent-state-3.json", ApplicationStartupState.INCONSISTENT,
                },
                // @formatter:on
            });
        }

        @Before
        public void loadApp() throws Exception {
            String appJson = TestUtil.getResourceAsString(pathToFileContainingAppJson, getClass());
            this.app = JsonUtil.fromJson(appJson, CloudApplication.class);

        }

        @Test
        public void testComputeCurrentState() {
            assertEquals(expectedState, new ApplicationStartupStateCalculator().computeCurrentState(app));
        }

    }

    @RunWith(Parameterized.class)
    public static class DesiredApplicationStartupStateCalculatorTest {

        private final String pathToFileContainingAppJson;
        private final boolean shouldNotStartAnyApp;
        private final ApplicationStartupState expectedState;
        private CloudApplication app;

        public DesiredApplicationStartupStateCalculatorTest(String pathToFileContainingAppJson, boolean shouldNotStartAnyApp,
                                                            ApplicationStartupState expectedState) {
            this.pathToFileContainingAppJson = pathToFileContainingAppJson;
            this.shouldNotStartAnyApp = shouldNotStartAnyApp;
            this.expectedState = expectedState;
        }

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
                // @formatter:off
                // (0)
                {
                    "app-with-no-start-attribute-true.json", true, ApplicationStartupState.STOPPED,
                },
                // (1)
                {
                    "app-with-no-start-attribute-true.json", false, ApplicationStartupState.STOPPED,
                },
                // (2)
                {
                    "app-with-no-start-attribute-false.json", true, ApplicationStartupState.STARTED,
                },
                // (3)
                {
                    "app-with-no-start-attribute-false.json", false, ApplicationStartupState.STARTED,
                },
                // (4)
                {
                    "app-without-no-start-attribute.json", true, ApplicationStartupState.STOPPED,
                },
                // (5)
                {
                    "app-without-no-start-attribute.json", false, ApplicationStartupState.STARTED,
                },
                // @formatter:on
            });
        }

        @Before
        public void loadApp() throws Exception {
            String appJson = TestUtil.getResourceAsString(pathToFileContainingAppJson, getClass());
            this.app = JsonUtil.fromJson(appJson, CloudApplication.class);

        }

        @Test
        public void testComputeCurrentState() {
            assertEquals(expectedState, new ApplicationStartupStateCalculator().computeDesiredState(app, shouldNotStartAnyApp));
        }

    }

}
