package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.Mockito.verify;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;

class UnbindServiceStepFromApplicationTest extends SyncFlowableStepTest<UnbindServiceFromApplicationStep> {

    private static final String APPLICATION_NAME = "test_application";
    private static final String SERVICE_NAME = "test_service";

    @Test
    void testUnbindServiceStep() {
        prepareContext();

        step.execute(execution);

        assertStepFinishedSuccessfully();
        verify(client).unbindServiceInstance(APPLICATION_NAME, SERVICE_NAME);
    }

    private void prepareContext() {
        CloudApplicationExtended application = ImmutableCloudApplicationExtended.builder()
                                                                                .name(APPLICATION_NAME)
                                                                                .build();
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.SERVICE_TO_UNBIND_BIND, SERVICE_NAME);
    }

    @Override
    protected UnbindServiceFromApplicationStep createStep() {
        return new UnbindServiceFromApplicationStep();
    }

}
