package org.cloudfoundry.multiapps.controller.core.cf.apps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.InstancesInfo;
import com.sap.cloudfoundry.client.facade.domain.InstanceState;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableInstancesInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableInstanceInfo;

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
                Arguments.of("app-in-inconsistent-state-2.json", ApplicationStartupState.INCONSISTENT)
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testComputeCurrentState(String pathToFileContainingAppJson, ApplicationStartupState expectedState) {
        String appJson = TestUtil.getResourceAsString(pathToFileContainingAppJson, getClass());
        StepInput input = JsonUtil.fromJson(appJson, StepInput.class);
        CloudApplication app = input.toCloudApplication();
        InstancesInfo instances = generateInstancesInfo(input.instances, input.runningInstances);
        assertEquals(expectedState, new ApplicationStartupStateCalculator().computeCurrentState(app, instances, input.env));
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
        StepInput input = JsonUtil.fromJson(appJson, StepInput.class);
        CloudApplication app = input.toCloudApplication();
        assertEquals(expectedState, new ApplicationStartupStateCalculator().computeDesiredState(app, input.env, shouldNotStartAnyApp));
    }

    private InstancesInfo generateInstancesInfo(int instances, int runningInstances) {
        var running = IntStream.range(0, runningInstances)
                               .mapToObj(i -> ImmutableInstanceInfo.builder()
                                                                   .state(InstanceState.RUNNING)
                                                                   .index(i)
                                                                   .build())
                               .collect(Collectors.toList());
        var starting = IntStream.range(runningInstances, instances)
                                .mapToObj(i -> ImmutableInstanceInfo.builder()
                                                                    .state(InstanceState.STARTING)
                                                                    .index(i)
                                                                    .build())
                                .collect(Collectors.toList());
        running.addAll(starting);
        return ImmutableInstancesInfo.builder()
                                     .instances(running)
                                     .build();
    }

    private static class StepInput {
        String name;
        Integer instances;
        Integer runningInstances;
        CloudApplication.State state;
        Map<String, String> env;

        CloudApplication toCloudApplication() {
            return ImmutableCloudApplication.builder()
                                            .metadata(ImmutableCloudMetadata.of(UUID.randomUUID()))
                                            .name(name)
                                            .state(state)
                                            .build();
        }
    }

}
