package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.List;

import org.cloudfoundry.multiapps.controller.core.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.util.HistoricOperationEventPersister;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class RetryProcessActionTest extends ProcessActionTest {

    @Mock
    private HistoricOperationEventPersister historicOperationEventPersister;
    @Mock
    private ProgressMessageService progressMessageService;

    @Test
    void testRetryActionWithoutAnyExceptions() {
        processAction.execute("fake-user", PROCESS_GUID);
        Mockito.verify(flowableFacade)
               .executeJob(PROCESS_GUID);
        verifySubprocessesAreExecuted();
        Mockito.verify(historicOperationEventPersister)
               .add(PROCESS_GUID, HistoricOperationEvent.EventType.RETRIED);
    }

    @Test
    void testRetryActionWithExceptionForRootProcess() {
        Mockito.doThrow(new RuntimeException())
               .when(flowableFacade)
               .executeJob(PROCESS_GUID);
        processAction.execute("fake-user", PROCESS_GUID);
        verifySubprocessesAreExecuted();
        Mockito.verify(historicOperationEventPersister)
               .add(PROCESS_GUID, HistoricOperationEvent.EventType.RETRIED);
    }

    private void verifySubprocessesAreExecuted() {
        Mockito.verify(flowableFacade)
               .executeJob(SUBPROCESS_1_ID);
        Mockito.verify(flowableFacade)
               .executeJob(SUBPROCESS_2_ID);
    }

    @Override
    protected ProcessAction createProcessAction() {
        return new RetryProcessAction(flowableFacade,
                                      List.of(new RetryProcessAdditionalAction(flowableFacade, progressMessageService),
                                              new SetRetryPhaseAdditionalProcessAction(flowableFacade)),
                                      historicOperationEventPersister,
                                      cloudControllerClientProvider);
    }

}
