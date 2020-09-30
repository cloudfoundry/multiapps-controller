package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.Collections;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class AbortProcessActionTest extends ProcessActionTest {

    @Mock
    private HistoricOperationEventService historicOperationEventService;
    @Mock
    private OperationService operationService;

    @BeforeEach
    void setUp() {
        prepareOperationService();
    }

    @Test
    void testAbortExecution() {
        processAction.execute(null, PROCESS_GUID);
        Mockito.verify(historicOperationEventService)
               .add(ImmutableHistoricOperationEvent.of(PROCESS_GUID, HistoricOperationEvent.EventType.ABORTED));
        Mockito.verify(historicOperationEventService)
               .add(ImmutableHistoricOperationEvent.of(PROCESS_GUID, HistoricOperationEvent.EventType.ABORT_EXECUTED));
    }

    private void prepareOperationService() {
        OperationQuery mockedOperationQuery = Mockito.mock(OperationQuery.class);
        Mockito.when(mockedOperationQuery.processId(PROCESS_GUID))
               .thenReturn(mockedOperationQuery);
        Operation operation = Mockito.mock(Operation.class);
        Mockito.when(mockedOperationQuery.singleResult())
               .thenReturn(operation);
        Mockito.when(operationService.createQuery())
               .thenReturn(mockedOperationQuery);
    }

    @Override
    protected ProcessAction createProcessAction() {
        return new AbortProcessAction(flowableFacade,
                                      Collections.emptyList(),
                                      historicOperationEventService,
                                      operationService,
                                      cloudControllerClientProvider);
    }
}
