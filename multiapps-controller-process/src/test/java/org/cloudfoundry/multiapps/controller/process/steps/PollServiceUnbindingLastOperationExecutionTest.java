package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;

class PollServiceUnbindingLastOperationExecutionTest extends AsyncStepOperationTest<UnbindServiceFromApplicationStep> {

    private static final String APP_TO_PROCESS_NAME = "test-app";
    private static final String SERVICE_TO_UNBIND_BIND = "test-service-instance";

    private AsyncExecutionState expectedExecutionStatus;

    @Test
    void testFailedExecutionForOptionalService() {
        expectedExecutionStatus = AsyncExecutionState.FINISHED;
        context.setVariable(Variables.SERVICES_TO_BIND, List.of(buildCloudServiceInstance(SERVICE_TO_UNBIND_BIND)));
        initializeParameters();
        testExecuteOperations();
    }

    private CloudServiceInstanceExtended buildCloudServiceInstance(String serviceInstanceName) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(serviceInstanceName)
                                                    .build();
    }

    private void initializeParameters() {
        context.setVariable(Variables.APP_TO_PROCESS, buildCloudApplication());
        context.setVariable(Variables.SERVICE_TO_UNBIND_BIND, SERVICE_TO_UNBIND_BIND);
    }

    private CloudApplicationExtended buildCloudApplication() {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_TO_PROCESS_NAME)
                                                .build();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return List.of(new PollServiceUnbindingLastOperationExecution());
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedExecutionStatus, result);
    }

    @Override
    protected UnbindServiceFromApplicationStep createStep() {
        return new UnbindServiceFromApplicationStep();
    }
}
