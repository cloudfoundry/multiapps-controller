package org.cloudfoundry.multiapps.controller.process.flowable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.cloudfoundry.multiapps.controller.persistence.query.ProgressMessageQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.flowable.engine.runtime.Execution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class RetryProcessAdditionalActionTest {

    private static final Supplier<String> RANDOM_UUID_SUPPLIER = () -> UUID.randomUUID()
                                                                           .toString();
    private static final String PROCESS_GUID = RANDOM_UUID_SUPPLIER.get();
    private static final String EXECUTION_1_ACTIVITY_ID = RANDOM_UUID_SUPPLIER.get();
    private static final String EXECUTION_2_ACTIVITY_ID = RANDOM_UUID_SUPPLIER.get();

    @Mock
    private FlowableFacade flowableFacade;
    @Mock
    private ProgressMessageService progressMessageService;
    @Mock
    private ProgressMessageQuery progressMessageQuery;
    @InjectMocks
    private RetryProcessAdditionalAction retryProcessAdditionalAction;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        prepareFlowableFacade();
        prepareProgressMessageService();
        prepareProgressMessageQuery();
    }

    @Test
    void testExecuteAdditionalAction() {
        retryProcessAdditionalAction.executeAdditionalProcessAction(PROCESS_GUID);
        Mockito.verify(progressMessageQuery, times(2))
               .delete();
    }

    private void prepareFlowableFacade() {
        List<Execution> mockedExecutions = getMockedExecutions();
        Mockito.when(flowableFacade.getActiveProcessExecutions(PROCESS_GUID))
               .thenReturn(mockedExecutions);
    }

    private List<Execution> getMockedExecutions() {
        Execution firstExecution = Mockito.mock(Execution.class);
        Mockito.when(firstExecution.getActivityId())
               .thenReturn(EXECUTION_1_ACTIVITY_ID);
        Execution secondExecution = Mockito.mock(Execution.class);
        Mockito.when(secondExecution.getActivityId())
               .thenReturn(EXECUTION_2_ACTIVITY_ID);
        return List.of(firstExecution, secondExecution);
    }

    private void prepareProgressMessageService() {
        Mockito.when(progressMessageService.createQuery())
               .thenReturn(progressMessageQuery);
    }

    private void prepareProgressMessageQuery() {
        Mockito.when(progressMessageQuery.processId(anyString()))
               .thenReturn(progressMessageQuery);
        Mockito.when(progressMessageQuery.taskId(anyString()))
               .thenReturn(progressMessageQuery);
        Mockito.when(progressMessageQuery.type(any()))
               .thenReturn(progressMessageQuery);
    }
}
