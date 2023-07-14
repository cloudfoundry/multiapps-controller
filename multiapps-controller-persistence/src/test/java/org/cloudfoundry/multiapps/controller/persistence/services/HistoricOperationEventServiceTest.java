package org.cloudfoundry.multiapps.controller.persistence.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent.EventType;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService.HistoricOperationEventMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HistoricOperationEventServiceTest {

    private final HistoricOperationEventService historicOperationEventService = createHistoricOperationEventService();

    private static final String PROCESS_ID = "processId_1";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final LocalDateTime DATE_1 = LocalDateTime.parse("2020-11-15T13:30:25.010Z", DATE_TIME_FORMATTER);
    private static final LocalDateTime DATE_2 = LocalDateTime.parse("2020-11-15T13:30:25.020Z", DATE_TIME_FORMATTER);

    private static final HistoricOperationEvent HISTORIC_OPERATION_1 = createHistoricOperationEvent(1, PROCESS_ID,
                                                                                                    HistoricOperationEvent.EventType.STARTED,
                                                                                                    DATE_1);
    private static final HistoricOperationEvent HISTORIC_OPERATION_2 = createHistoricOperationEvent(2, PROCESS_ID,
                                                                                                    HistoricOperationEvent.EventType.FINISHED,
                                                                                                    DATE_2);
    private static final HistoricOperationEvent HISTORIC_OPERATION_3 = createHistoricOperationEvent(3, "processId_2",
                                                                                                    HistoricOperationEvent.EventType.STARTED,
                                                                                                    DATE_2);

    @AfterEach
    void cleanUp() {
        historicOperationEventService.createQuery()
                                     .delete();
    }

    @Test
    void testAdd() {
        historicOperationEventService.add(HISTORIC_OPERATION_1);
        List<HistoricOperationEvent> historicOperations = historicOperationEventService.createQuery()
                                                                                       .list();
        assertEquals(1, historicOperations.size());
        verifyHistoricOperationsAreEqual(HISTORIC_OPERATION_1, historicOperations.get(0));
    }

    @Test
    void testFindById() {
        historicOperationEventService.add(HISTORIC_OPERATION_1);
        verifyHistoricOperationsAreEqual(HISTORIC_OPERATION_1, historicOperationEventService.createQuery()
                                                                                            .id(1L)
                                                                                            .singleResult());
    }

    @Test
    void testFindByType() {
        historicOperationEventService.add(HISTORIC_OPERATION_1);
        List<HistoricOperationEvent> historicOperations = historicOperationEventService.createQuery()
                                                                                       .type(HistoricOperationEvent.EventType.STARTED)
                                                                                       .list();
        assertEquals(1, historicOperations.size());
        verifyHistoricOperationsAreEqual(HISTORIC_OPERATION_1, historicOperations.get(0));
    }

    @Test
    void testFindByProcessId() {
        historicOperationEventService.add(HISTORIC_OPERATION_1);
        historicOperationEventService.add(HISTORIC_OPERATION_2);
        historicOperationEventService.add(HISTORIC_OPERATION_3);
        List<HistoricOperationEvent> historicOperations = historicOperationEventService.createQuery()
                                                                                       .processId(PROCESS_ID)
                                                                                       .list();
        assertEquals(2, historicOperations.size());
        verifyHistoricOperationsAreEqual(HISTORIC_OPERATION_1, historicOperations.get(0));
        verifyHistoricOperationsAreEqual(HISTORIC_OPERATION_2, historicOperations.get(1));
    }

    @Test
    void testDeleteByProcessId() {
        historicOperationEventService.add(HISTORIC_OPERATION_1);
        historicOperationEventService.add(HISTORIC_OPERATION_2);
        historicOperationEventService.add(HISTORIC_OPERATION_3);
        assertEquals(2, historicOperationEventService.createQuery()
                                                     .processId(PROCESS_ID)
                                                     .delete());
        List<HistoricOperationEvent> historicOperations = historicOperationEventService.createQuery()
                                                                                       .list();
        assertEquals(1, historicOperations.size());
        verifyHistoricOperationsAreEqual(HISTORIC_OPERATION_3, historicOperations.get(0));
    }

    @Test
    void testFindOlderThanOperations() {
        historicOperationEventService.add(HISTORIC_OPERATION_1);
        historicOperationEventService.add(HISTORIC_OPERATION_2);
        historicOperationEventService.add(HISTORIC_OPERATION_3);
        List<HistoricOperationEvent> historicOperations = historicOperationEventService.createQuery()
                                                                                       .olderThan(DATE_2)
                                                                                       .list();
        assertEquals(1, historicOperations.size());
        verifyHistoricOperationsAreEqual(HISTORIC_OPERATION_1, historicOperations.get(0));

    }

    @Test
    void testOrderByTimestamp() {
        historicOperationEventService.add(HISTORIC_OPERATION_1);
        historicOperationEventService.add(HISTORIC_OPERATION_2);
        List<HistoricOperationEvent> historicOperations = historicOperationEventService.createQuery()
                                                                                       .orderByTimestamp(OrderDirection.ASCENDING)
                                                                                       .list();
        assertEquals(2, historicOperations.size());
        verifyHistoricOperationsAreEqual(HISTORIC_OPERATION_1, historicOperations.get(0));
        verifyHistoricOperationsAreEqual(HISTORIC_OPERATION_2, historicOperations.get(1));
    }

    @Test
    void testOrderOlderTimestampWithHigherIndex() {
        historicOperationEventService.add(HISTORIC_OPERATION_2);
        HistoricOperationEvent olderHistoricOperationEvent = createHistoricOperationEvent(3, PROCESS_ID, EventType.STARTED,
                                                                                          LocalDateTime.parse("2020-11-15T13:30:24.010Z",
                                                                                                              DATE_TIME_FORMATTER));
        historicOperationEventService.add(olderHistoricOperationEvent);
        List<HistoricOperationEvent> historicOperations = historicOperationEventService.createQuery()
                                                                                       .orderByTimestamp(OrderDirection.ASCENDING)
                                                                                       .list();
        assertEquals(2, historicOperations.size());
        verifyHistoricOperationsAreEqual(olderHistoricOperationEvent, historicOperations.get(0));
        verifyHistoricOperationsAreEqual(HISTORIC_OPERATION_2, historicOperations.get(1));
    }

    @Test
    void testThrowExceptionOnConflictingEntity() {
        historicOperationEventService.add(HISTORIC_OPERATION_1);
        assertThrows(ConflictException.class, () -> historicOperationEventService.add(HISTORIC_OPERATION_1));
    }

    @Test
    void testThrowExceptionOnEntityNotFound() {
        assertThrows(NotFoundException.class, () -> historicOperationEventService.update(HISTORIC_OPERATION_1, HISTORIC_OPERATION_2));
    }

    private static ImmutableHistoricOperationEvent
            createHistoricOperationEvent(long id, String processId, HistoricOperationEvent.EventType type, LocalDateTime timeStamp) {
        return ImmutableHistoricOperationEvent.builder()
                                              .id(id)
                                              .processId(processId)
                                              .type(type)
                                              .timestamp(timeStamp)
                                              .build();
    }

    private HistoricOperationEventService createHistoricOperationEventService() {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("TestDefault");
        HistoricOperationEventService historicOperationEventService = new HistoricOperationEventService(entityManagerFactory);
        historicOperationEventService.historicOperationEventMapper = new HistoricOperationEventMapper();
        return historicOperationEventService;
    }

    private void verifyHistoricOperationsAreEqual(HistoricOperationEvent expectedOperationEvent,
                                                  HistoricOperationEvent actualOperationEvent) {
        assertEquals(expectedOperationEvent.getId(), actualOperationEvent.getId());
        assertEquals(expectedOperationEvent.getType(), actualOperationEvent.getType());
        assertEquals(expectedOperationEvent.getProcessId(), actualOperationEvent.getProcessId());
        assertEquals(expectedOperationEvent.getTimestamp(), actualOperationEvent.getTimestamp());
    }
}
