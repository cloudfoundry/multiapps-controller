package org.cloudfoundry.multiapps.controller.process.flowable;

import static org.mockito.Mockito.times;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.runtime.Execution;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ResumeProcessActionTest extends ProcessActionTest {

    @Test
    void testResumeExecutionWithExecutionsAtReceiveTask() {
        processAction.execute("fake-user", PROCESS_GUID);
        Mockito.verify(flowableFacade, times(2))
               .trigger(EXECUTION_ID, Map.of(Variables.USER.getName(), "fake-user"));
        assertStateUpdated(Operation.State.RUNNING);
    }

    @Test
    void testResumeExecutionWithoutExecutionsAtReceiveTask() {
        List<Execution> mockedExecutions = getMockedExecutions();
        Mockito.when(flowableFacade.findExecutionsAtReceiveTask(PROCESS_GUID))
               .thenReturn(mockedExecutions)
               .thenReturn(Collections.emptyList());
        processAction.execute("fake-user", PROCESS_GUID);
        Mockito.verify(flowableFacade)
               .trigger(EXECUTION_ID, Map.of(Variables.USER.getName(), "fake-user"));
        assertStateUpdated(Operation.State.RUNNING);
    }

    @Override
    protected ProcessAction createProcessAction() {
        return new ResumeProcessAction(flowableFacade, Collections.emptyList(), operationService, cloudControllerClientProvider);
    }
}
