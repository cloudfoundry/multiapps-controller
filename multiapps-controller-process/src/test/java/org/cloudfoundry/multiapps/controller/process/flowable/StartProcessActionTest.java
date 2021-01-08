package org.cloudfoundry.multiapps.controller.process.flowable;

import static org.mockito.Mockito.times;

import java.util.Collections;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StartProcessActionTest extends ProcessActionTest {

    @Test
    void testResumeExecution() {
        processAction.execute("fake-user", PROCESS_GUID);
        Mockito.verify(flowableFacade, times(2))
               .trigger(EXECUTION_ID, Map.of(Variables.USER.getName(), "fake-user"));
        assertStateUpdated(Operation.State.RUNNING);
    }

    @Override
    protected ProcessAction createProcessAction() {
        return new StartProcessAction(flowableFacade, Collections.emptyList(), operationService, cloudControllerClientProvider);
    }
}
