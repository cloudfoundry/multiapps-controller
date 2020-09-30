package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class RetryProcessActionTest extends ProcessActionTest {

    @Mock
    private HistoricOperationEventService historicOperationEventService;
    @Mock
    private ProgressMessageService progressMessageService;

    @Test
    void testRetryActionWithoutAnyExceptions() {
        processAction.execute("fake-user", PROCESS_GUID);
        Mockito.verify(flowableFacade)
               .executeJob(PROCESS_GUID);
        verifySubprocessesAreExecuted();
        Mockito.verify(historicOperationEventService)
               .add(ImmutableHistoricOperationEvent.of(PROCESS_GUID, HistoricOperationEvent.EventType.RETRIED));
    }

    @Test
    void testRetryActionWithExceptionForRootProcess() {
        Mockito.doThrow(new RuntimeException())
               .when(flowableFacade)
               .executeJob(PROCESS_GUID);
        processAction.execute("fake-user", PROCESS_GUID);
        verifySubprocessesAreExecuted();
        Mockito.verify(historicOperationEventService)
               .add(ImmutableHistoricOperationEvent.of(PROCESS_GUID, HistoricOperationEvent.EventType.RETRIED));
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
                                      historicOperationEventService,
                                      cloudControllerClientProvider);
    }

}
