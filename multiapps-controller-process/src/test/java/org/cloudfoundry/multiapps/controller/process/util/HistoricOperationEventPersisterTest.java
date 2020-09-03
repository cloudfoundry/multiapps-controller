package org.cloudfoundry.multiapps.controller.process.util;

import java.util.UUID;

import org.cloudfoundry.multiapps.controller.core.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.core.persistence.service.HistoricOperationEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class HistoricOperationEventPersisterTest {

    private static final String PROCESS_GUID = UUID.randomUUID()
                                                   .toString();

    @Mock
    private HistoricOperationEventService configurationSubscriptionService;
    @InjectMocks
    private HistoricOperationEventPersister historicOperationEventPersister;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testAddingHistoricOperationEvent() {
        HistoricOperationEvent historicOperationEvent = ImmutableHistoricOperationEvent.builder()
                                                                                       .processId(PROCESS_GUID)
                                                                                       .type(HistoricOperationEvent.EventType.FINISHED)
                                                                                       .build();
        historicOperationEventPersister.add(PROCESS_GUID, HistoricOperationEvent.EventType.FINISHED);
        Mockito.verify(configurationSubscriptionService)
               .add(historicOperationEvent);
    }

}
