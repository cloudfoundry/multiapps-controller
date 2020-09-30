package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class ScaleAppStepTest extends SyncFlowableStepTest<ScaleAppStep> {

    public static Stream<Arguments> testExecute() {
        return Stream.of(
        // @formatter:off
            Arguments.of(new SimpleApplication("test-app-1", 2), new SimpleApplication("test-app-1", 3)),
            Arguments.of(new SimpleApplication("test-app-1", 2), new SimpleApplication("test-app-1", 2)),
            Arguments.of(new SimpleApplication("test-app-1", 2), null)
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(SimpleApplication application, SimpleApplication existingApplication) {
        initializeParameters(application, existingApplication);

        step.execute(execution);

        assertStepFinishedSuccessfully();

        validateUpdatedApplications(application, existingApplication);
    }

    private void initializeParameters(SimpleApplication application, SimpleApplication existingApplication) {
        context.setVariable(Variables.MODULES_INDEX, 0);
        context.setVariable(Variables.APP_TO_PROCESS, application.toCloudApplication());
        context.setVariable(Variables.APPS_TO_DEPLOY, Collections.emptyList());
        context.setVariable(Variables.EXISTING_APP, (existingApplication != null) ? existingApplication.toCloudApplication() : null);
    }

    private void validateUpdatedApplications(SimpleApplication application, SimpleApplication existingApplication) {
        if (application.instances != 0) {
            if (existingApplication == null || application.instances != existingApplication.instances) {
                Mockito.verify(client)
                       .updateApplicationInstances(application.name, application.instances);
            }
        }
    }

    static class SimpleApplication {
        final String name;
        final int instances;

        public SimpleApplication(String name, int instances) {
            this.name = name;
            this.instances = instances;
        }

        CloudApplicationExtended toCloudApplication() {
            return ImmutableCloudApplicationExtended.builder()
                                                    .name(name)
                                                    .moduleName(name)
                                                    .instances(instances)
                                                    .build();
        }
    }

    @Override
    protected ScaleAppStep createStep() {
        return new ScaleAppStep();
    }

}
