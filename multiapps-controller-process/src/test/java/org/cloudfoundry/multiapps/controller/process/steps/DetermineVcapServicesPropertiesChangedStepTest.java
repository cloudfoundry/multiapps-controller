package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.listeners.ManageAppServiceBindingEndListener;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DetermineVcapServicesPropertiesChangedStepTest extends SyncFlowableStepTest<DetermineVcapServicesPropertiesChangedStep> {

    private static final String APP_NAME = "test_application";

    static Stream<Arguments> testStep() {
        return Stream.of(Arguments.of(Map.of("service-1", true, "service-2", true), true),
                         Arguments.of(Map.of("service-1", false, "service-2", false), false),
                         Arguments.of(Map.of("service-1", true, "service-2", false), true), Arguments.of(Map.of("service-1", true), true),
                         Arguments.of(Map.of("service-1", false), false), Arguments.of(Collections.emptyMap(), false));
    }

    @ParameterizedTest
    @MethodSource
    void testStep(Map<String, Boolean> services, boolean expectedVcapPropertiesChangedValue) {
        prepareContext(services);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(expectedVcapPropertiesChangedValue, context.getVariable(Variables.VCAP_SERVICES_PROPERTIES_CHANGED));
    }

    private void prepareContext(Map<String, Boolean> services) {
        CloudApplicationExtended application = buildApplication();
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.SERVICES_TO_UNBIND_BIND, List.copyOf(services.keySet()));
        services.forEach((key, value) -> execution.setVariable(ManageAppServiceBindingEndListener.buildExportedVariableName(APP_NAME, key),
                                                               value));
    }

    private CloudApplicationExtended buildApplication() {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_NAME)
                                                .build();
    }

    @Override
    protected DetermineVcapServicesPropertiesChangedStep createStep() {
        return new DetermineVcapServicesPropertiesChangedStep();
    }

}
