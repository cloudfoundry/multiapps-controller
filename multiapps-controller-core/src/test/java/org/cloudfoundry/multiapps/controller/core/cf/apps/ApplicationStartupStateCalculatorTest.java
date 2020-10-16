package org.cloudfoundry.multiapps.controller.core.cf.apps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

class ApplicationStartupStateCalculatorTest {

    public static Stream<Arguments> testComputeCurrentState() {
        return Stream.of(
        // @formatter:off
                // (0)
                Arguments.of("started-app.json", ApplicationStartupState.STARTED),
                // (1)
                Arguments.of("stopped-app.json", ApplicationStartupState.STOPPED),
                // (2) The number of running instances is different than the number of total instances:
                Arguments.of("app-in-inconsistent-state-0.json", ApplicationStartupState.INCONSISTENT),
                // (3) The number of running instances is not zero when the requested state is stopped:
                Arguments.of("app-in-inconsistent-state-1.json", ApplicationStartupState.INCONSISTENT),
                // (4) The number of running and the number of total instances is zero, but the requested state is started:
                Arguments.of("app-in-inconsistent-state-2.json", ApplicationStartupState.INCONSISTENT),
                // (5) The number of running instances is bigger than the number of total instances:
                Arguments.of("app-in-inconsistent-state-3.json", ApplicationStartupState.INCONSISTENT)
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testComputeCurrentState(String pathToFileContainingAppJson, ApplicationStartupState expectedState) {
        String appJson = TestUtil.getResourceAsString(pathToFileContainingAppJson, getClass());
        CloudApplication app = JsonUtil.fromJson(appJson, CloudApplication.class);
        assertEquals(expectedState, new ApplicationStartupStateCalculator().computeCurrentState(app));
    }

    public static Stream<Arguments> testComputeDesiredState() {
        return Stream.of(
        // @formatter:off
                // (0)
                Arguments.of("app-with-no-start-attribute-true.json", true, ApplicationStartupState.STOPPED),
                // (1)
                Arguments.of("app-with-no-start-attribute-true.json", false, ApplicationStartupState.STOPPED),
                // (2)
                Arguments.of("app-with-no-start-attribute-false.json", true, ApplicationStartupState.STARTED),
                // (3)
                Arguments.of("app-with-no-start-attribute-false.json", false, ApplicationStartupState.STARTED),
                // (4)
                Arguments.of("app-without-no-start-attribute.json", true, ApplicationStartupState.STOPPED),
                // (5)
                Arguments.of("app-without-no-start-attribute.json", false, ApplicationStartupState.STARTED)
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testComputeDesiredState(String pathToFileContainingAppJson, boolean shouldNotStartAnyApp, ApplicationStartupState expectedState) {
        String appJson = TestUtil.getResourceAsString(pathToFileContainingAppJson, getClass());
        CloudApplication app = JsonUtil.fromJson(appJson, CloudApplication.class);
        assertEquals(expectedState, new ApplicationStartupStateCalculator().computeDesiredState(app, shouldNotStartAnyApp));
    }

}
