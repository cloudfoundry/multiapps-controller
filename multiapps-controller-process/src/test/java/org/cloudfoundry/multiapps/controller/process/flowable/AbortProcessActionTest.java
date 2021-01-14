package org.cloudfoundry.multiapps.controller.process.flowable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage.ProgressMessageType;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.ProgressMessageQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatraceProcessEvent;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatracePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

class AbortProcessActionTest extends ProcessActionTest {

    @Mock
    private HistoricOperationEventService historicOperationEventService;

    @Mock
    private ProgressMessageService progressMessageService;
    @Mock
    private ProgressMessageQuery progressMessageQuery;
    @Mock
    private ProgressMessage progressMessage;
    @Mock
    private DynatracePublisher dynatracePublisher;

    private static final String SPACE_ID = "9ba1dfc7-9c2c-40d5-8bf9-fd04fa7a1722";
    private static final String MTA_ID = "my-mta";
    private static final Operation OPERATION = ImmutableOperation.builder()
                                                                 .mtaId(MTA_ID)
                                                                 .spaceId(SPACE_ID)
                                                                 .processType(ProcessType.DEPLOY)
                                                                 .state(Operation.State.RUNNING)
                                                                 .build();
    @BeforeEach
    void setUp() {
        prepareOperationService();
        prepareProgressMessageService();
    }

    @Test
    void testAbortExecution() {
        processAction.execute(null, PROCESS_GUID);
        Mockito.verify(historicOperationEventService)
               .add(ImmutableHistoricOperationEvent.of(PROCESS_GUID, HistoricOperationEvent.EventType.ABORTED));
        Mockito.verify(historicOperationEventService)
               .add(ImmutableHistoricOperationEvent.of(PROCESS_GUID, HistoricOperationEvent.EventType.ABORT_EXECUTED));
    }

    @Test
    void testAbortActionAndPublishingDynatraceEvent() {
        ProcessType processType = ProcessType.DEPLOY;
        Mockito.when(progressMessageQuery.list())
               .thenReturn(List.of(progressMessage));
        processAction.execute(null, PROCESS_GUID);
        ArgumentCaptor<DynatraceProcessEvent> argumentCaptor = ArgumentCaptor.forClass(DynatraceProcessEvent.class);
        Mockito.verify(dynatracePublisher)
               .publishProcessEvent(argumentCaptor.capture(), Mockito.any());
        DynatraceProcessEvent actualDynatraceEvent = argumentCaptor.getValue();
        assertEquals(MTA_ID, actualDynatraceEvent.getMtaId());
        assertEquals(SPACE_ID, actualDynatraceEvent.getSpaceId());
        assertEquals(processType, actualDynatraceEvent.getProcessType());
        assertEquals(DynatraceProcessEvent.EventType.FAILED, actualDynatraceEvent.getEventType());
    }

    @Test
    void testAbortActionNoPublishingDynatraceEvent() {
        Mockito.when(progressMessageQuery.list())
               .thenReturn(Collections.emptyList());
        processAction.execute(null, PROCESS_GUID);
        Mockito.verify(dynatracePublisher, Mockito.never())
               .publishProcessEvent(Mockito.any(), Mockito.any());
    }

    private void prepareOperationService() {
        OperationQuery mockedOperationQuery = Mockito.mock(OperationQuery.class);
        Mockito.when(mockedOperationQuery.processId(PROCESS_GUID))
               .thenReturn(mockedOperationQuery);
        Mockito.when(mockedOperationQuery.singleResult())
               .thenReturn(OPERATION);
        Mockito.when(operationService.createQuery())
               .thenReturn(mockedOperationQuery);
    }

    private void prepareProgressMessageService() {
        Mockito.when(progressMessageService.createQuery())
               .thenReturn(progressMessageQuery);
        Mockito.when(progressMessageQuery.processId(PROCESS_GUID))
               .thenReturn(progressMessageQuery);
        Mockito.when(progressMessageQuery.type(ProgressMessageType.ERROR))
               .thenReturn(progressMessageQuery);
    }

    @Override
    protected ProcessAction createProcessAction() {
        return new AbortProcessAction(flowableFacade,
                                      Collections.emptyList(),
                                      historicOperationEventService,
                                      operationService,
                                      cloudControllerClientProvider,
                                      progressMessageService,
                                      dynatracePublisher);
    }
}
