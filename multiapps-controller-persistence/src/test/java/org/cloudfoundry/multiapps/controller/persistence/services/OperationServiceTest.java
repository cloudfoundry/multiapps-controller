package org.cloudfoundry.multiapps.controller.persistence.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService.OperationMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OperationServiceTest {

    private static final Operation OPERATION_1 = createOperation("1", ProcessType.DEPLOY, "spaceId", "mtaId", "user", false,
                                                                 ZonedDateTime.parse("2010-10-08T10:00:00.000Z[UTC]"),
                                                                 ZonedDateTime.parse("2010-10-14T10:00:00.000Z[UTC]"));
    private static final Operation OPERATION_2 = createOperation("2", ProcessType.UNDEPLOY, "spaceId1", "mtaId1", "user1", true,
                                                                 ZonedDateTime.parse("2010-10-10T10:00:00.000Z[UTC]"),
                                                                 ZonedDateTime.parse("2010-10-12T10:00:00.000Z[UTC]"));
    private final OperationService operationService = createOperationService();

    @AfterEach
    void cleanUp() {
        operationService.createQuery()
                        .delete();
    }

    @Test
    void testAdd() {
        operationService.add(OPERATION_1);
        assertEquals(Collections.singletonList(OPERATION_1), operationService.createQuery()
                                                                             .list());
        assertEquals(OPERATION_1, operationService.createQuery()
                                                  .processId(OPERATION_1.getProcessId())
                                                  .singleResult());
    }

    @Test
    void testAddWithNonEmptyDatabase() {
        addOperations(List.of(OPERATION_1, OPERATION_2));

        assertOperationExists(OPERATION_1.getProcessId());
        assertOperationExists(OPERATION_2.getProcessId());

        assertEquals(List.of(OPERATION_1, OPERATION_2), operationService.createQuery()
                                                                        .list());
    }

    @Test
    void testAddWithAlreadyExistingOperation() {
        operationService.add(OPERATION_1);
        Exception exception = assertThrows(ConflictException.class, () -> operationService.add(OPERATION_1));
        String expectedExceptionMessage = MessageFormat.format(Messages.OPERATION_ALREADY_EXISTS, OPERATION_1.getProcessId());
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    void testQueryByProcessId() {
        testQueryByCriteria((query, operation) -> query.processId(operation.getProcessId()), OPERATION_1, OPERATION_2);
    }

    @Test
    void testQueryByProcessType() {
        testQueryByCriteria((query, operation) -> query.processType(operation.getProcessType()), OPERATION_1, OPERATION_2);
    }

    @Test
    void testQueryBySpaceId() {
        testQueryByCriteria((query, operation) -> query.spaceId(operation.getSpaceId()), OPERATION_1, OPERATION_2);
    }

    @Test
    void testQueryByMtaId() {
        testQueryByCriteria((query, operation) -> query.mtaId(operation.getMtaId()), OPERATION_1, OPERATION_2);
    }

    @Test
    void testQueryByUser() {
        testQueryByCriteria((query, operation) -> query.user(operation.getUser()), OPERATION_1, OPERATION_2);
    }

    @Test
    void testQueryByAcquiredLock() {
        testQueryByCriteria((query, operation) -> query.acquiredLock(operation.hasAcquiredLock()), OPERATION_1, OPERATION_2);
    }

    @Test
    void testQueryByState() {
        Operation operation1 = ImmutableOperation.copyOf(OPERATION_1)
                                                 .withState(Operation.State.ERROR);
        Operation operation2 = ImmutableOperation.copyOf(OPERATION_2)
                                                 .withState(Operation.State.RUNNING);
        testQueryByCriteria((query, operation) -> query.state(operation.getState()), operation1, operation2);
    }

    @Test
    void testQueryByStartedBefore() {
        testQueryByCriteria((query, operation) -> query.startedBefore(toDate(ZonedDateTime.parse("2010-10-09T00:00:00.000Z[UTC]"))),
                            OPERATION_1, OPERATION_2);
    }

    @Test
    void testQueryByEndedAfter() {
        testQueryByCriteria((query, operation) -> query.endedAfter(toDate(ZonedDateTime.parse("2010-10-13T10:00:00.000Z[UTC]"))),
                            OPERATION_1, OPERATION_2);
    }

    @Test
    void testQueryWithStateAnyOf() {
        Operation operation1 = ImmutableOperation.copyOf(OPERATION_1)
                                                 .withState(Operation.State.ERROR);
        Operation operation2 = ImmutableOperation.copyOf(OPERATION_2)
                                                 .withState(Operation.State.RUNNING);
        testQueryByCriteria((query, operation) -> query.withStateAnyOf(Collections.singletonList(Operation.State.ERROR)), operation1,
                            operation2);
    }

    @Test
    void testQueryInNonFinalState() {
        Operation operation2 = ImmutableOperation.copyOf(OPERATION_2)
                                                 .withState(Operation.State.ABORTED);
        testQueryByCriteria((query, operation) -> query.inNonFinalState(), OPERATION_1, operation2);
    }

    @Test
    void testQueryInFinalState() {
        Operation operation1 = ImmutableOperation.copyOf(OPERATION_1)
                                                 .withState(Operation.State.FINISHED);
        testQueryByCriteria((query, operation) -> query.inFinalState(), operation1, OPERATION_2);
    }

    private void testQueryByCriteria(OperationQueryBuilder operationQueryBuilder, Operation operation1, Operation operation2) {
        addOperations(List.of(operation1, operation2));
        assertEquals(1, operationQueryBuilder.build(operationService.createQuery(), operation1)
                                             .list()
                                             .size());
        assertEquals(1, operationQueryBuilder.build(operationService.createQuery(), operation1)
                                             .delete());
        assertOperationExists(operation2.getProcessId());
    }

    private interface OperationQueryBuilder {

        OperationQuery build(OperationQuery operationQuery, Operation testedOperation);
    }

    private void addOperations(List<Operation> operations) {
        operations.forEach(operationService::add);
    }

    private void assertOperationExists(String processId) {
        // If does not exist, will throw NoResultException
        operationService.createQuery()
                        .processId(processId)
                        .singleResult();
    }

    private static Operation createOperation(String processId, ProcessType type, String spaceId, String mtaId, String user,
                                             boolean acquiredLock, ZonedDateTime startedAt, ZonedDateTime endedAt) {
        return ImmutableOperation.builder()
                                 .processId(processId)
                                 .processType(type)
                                 .spaceId(spaceId)
                                 .mtaId(mtaId)
                                 .user(user)
                                 .hasAcquiredLock(acquiredLock)
                                 .startedAt(startedAt)
                                 .endedAt(endedAt)
                                 .build();
    }

    private Date toDate(ZonedDateTime zonedDateTime) {
        return Date.from(zonedDateTime.toInstant());
    }

    private OperationService createOperationService() {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("TestDefault");
        OperationService operationService = new OperationService(entityManagerFactory);
        operationService.operationMapper = new OperationMapper();
        return operationService;
    }

}
