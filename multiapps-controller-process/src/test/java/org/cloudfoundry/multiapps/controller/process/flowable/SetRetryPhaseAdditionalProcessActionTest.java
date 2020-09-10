package org.cloudfoundry.multiapps.controller.process.flowable;

import static org.mockito.Mockito.never;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.runtime.Execution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class SetRetryPhaseAdditionalProcessActionTest {

    private static final Supplier<String> RANDOM_UUID_SUPPLIER = () -> UUID.randomUUID()
                                                                           .toString();
    private static final String PROCESS_GUID = RANDOM_UUID_SUPPLIER.get();
    private static final String EXECUTION_WITHOUT_DEADLETTER_JOBS_PROCESS_ID = RANDOM_UUID_SUPPLIER.get();
    private static final String EXECUTION_WITH_DEADLETTER_JOBS_PROCESS_ID = RANDOM_UUID_SUPPLIER.get();

    @Mock
    private FlowableFacade flowableFacade;
    @Mock
    private ProcessEngine processEngine;
    @Mock
    private RuntimeService runtimeService;
    @InjectMocks
    private SetRetryPhaseAdditionalProcessAction setRetryPhaseAdditionalProcessAction;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        prepareProcessEngine();
        prepareFlowableFacade();
    }

    @Test
    void testExecuteAdditionalProcessAction() {
        setRetryPhaseAdditionalProcessAction.executeAdditionalProcessAction(PROCESS_GUID);
        Mockito.verify(runtimeService)
               .setVariable(EXECUTION_WITH_DEADLETTER_JOBS_PROCESS_ID, Variables.STEP_PHASE.getName(), StepPhase.RETRY.name());
        Mockito.verify(runtimeService, never())
               .setVariable(EXECUTION_WITHOUT_DEADLETTER_JOBS_PROCESS_ID, Variables.STEP_PHASE.getName(), StepPhase.RETRY.name());
    }

    private void prepareFlowableFacade() {
        List<Execution> mockedExecutions = getMockedExecutions();
        Mockito.when(flowableFacade.getActiveProcessExecutions(PROCESS_GUID))
               .thenReturn(mockedExecutions);
        Mockito.when(flowableFacade.getProcessEngine())
               .thenReturn(processEngine);
    }

    private List<Execution> getMockedExecutions() {
        Execution executionWithoutDeadLetterJobs = Mockito.mock(ExecutionEntityImpl.class);
        Mockito.when(executionWithoutDeadLetterJobs.getProcessInstanceId())
               .thenReturn(EXECUTION_WITHOUT_DEADLETTER_JOBS_PROCESS_ID);
        ExecutionEntityImpl executionWithDeadLetterJobs = Mockito.mock(ExecutionEntityImpl.class);
        Mockito.when(executionWithDeadLetterJobs.getProcessInstanceId())
               .thenReturn(EXECUTION_WITH_DEADLETTER_JOBS_PROCESS_ID);
        Mockito.when(executionWithDeadLetterJobs.getDeadLetterJobCount())
               .thenReturn(1);
        return List.of(executionWithoutDeadLetterJobs, executionWithDeadLetterJobs);
    }

    private void prepareProcessEngine() {
        Mockito.when(processEngine.getRuntimeService())
               .thenReturn(runtimeService);
    }
}
