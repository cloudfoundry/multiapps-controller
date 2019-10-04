package com.sap.cloud.lm.sl.cf.core.persistence.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.persistence.query.OperationQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService.OperationMapper;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.ConflictException;

public class OperationServiceTest {

    private static final Operation OPERATION_1 = createOperation("1", ProcessType.DEPLOY, "spaceId", "mtaId", "user", false,
                                                                 ZonedDateTime.parse("2010-10-08T10:00:00.000Z[UTC]"),
                                                                 ZonedDateTime.parse("2010-10-14T10:00:00.000Z[UTC]"));
    private static final Operation OPERATION_2 = createOperation("2", ProcessType.UNDEPLOY, "spaceId1", "mtaId1", "user1", true,
                                                                 ZonedDateTime.parse("2010-10-10T10:00:00.000Z[UTC]"),
                                                                 ZonedDateTime.parse("2010-10-12T10:00:00.000Z[UTC]"));
    private OperationService operationService = createOperationService();

    @AfterEach
    public void cleanUp() {
        operationService.createQuery()
                        .delete();
    }

    @Test
    public void testAdd() {
        operationService.add(OPERATION_1);
        assertEquals(Arrays.asList(OPERATION_1), operationService.createQuery()
                                                                 .list());
        assertEquals(OPERATION_1, operationService.createQuery()
                                                  .processId(OPERATION_1.getProcessId())
                                                  .singleResult());
    }

    @Test
    public void testAddWithNonEmptyDatabase() {
        addOperations(Arrays.asList(OPERATION_1, OPERATION_2));

        assertOperationExists(OPERATION_1.getProcessId());
        assertOperationExists(OPERATION_2.getProcessId());

        assertEquals(Arrays.asList(OPERATION_1, OPERATION_2), operationService.createQuery()
                                                                              .list());
    }

    @Test
    public void testAddWithAlreadyExistingOperation() {
        operationService.add(OPERATION_1);
        Exception exception = assertThrows(ConflictException.class, () -> operationService.add(OPERATION_1));
        String expectedExceptionMessage = MessageFormat.format(Messages.OPERATION_ALREADY_EXISTS, OPERATION_1.getProcessId());
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void testQueryByProcessId() {
        testQueryByCriteria((query, operation) -> query.processId(operation.getProcessId()), OPERATION_1, OPERATION_2);
    }

    @Test
    public void testQueryByProcessType() {
        testQueryByCriteria((query, operation) -> query.processType(operation.getProcessType()), OPERATION_1, OPERATION_2);
    }

    @Test
    public void testQueryBySpaceId() {
        testQueryByCriteria((query, operation) -> query.spaceId(operation.getSpaceId()), OPERATION_1, OPERATION_2);
    }

    @Test
    public void testQueryByMtaId() {
        testQueryByCriteria((query, operation) -> query.mtaId(operation.getMtaId()), OPERATION_1, OPERATION_2);
    }

    @Test
    public void testQueryByUser() {
        testQueryByCriteria((query, operation) -> query.user(operation.getUser()), OPERATION_1, OPERATION_2);
    }

    @Test
    public void testQueryByAcquiredLock() {
        testQueryByCriteria((query, operation) -> query.acquiredLock(operation.hasAcquiredLock()), OPERATION_1, OPERATION_2);
    }

    @Test
    public void testQueryByState() {
        Operation operation1 = ImmutableOperation.copyOf(OPERATION_1)
                                                 .withState(State.ERROR);
        Operation operation2 = ImmutableOperation.copyOf(OPERATION_2)
                                                 .withState(State.RUNNING);
        testQueryByCriteria((query, operation) -> query.state(operation.getState()), operation1, operation2);
    }

    @Test
    public void testQueryByStartedBefore() {
        testQueryByCriteria((query, operation) -> query.startedBefore(toDate(ZonedDateTime.parse("2010-10-09T00:00:00.000Z[UTC]"))),
                            OPERATION_1, OPERATION_2);
    }

    @Test
    public void testQueryByEndedAfter() {
        testQueryByCriteria((query, operation) -> query.endedAfter(toDate(ZonedDateTime.parse("2010-10-13T10:00:00.000Z[UTC]"))),
                            OPERATION_1, OPERATION_2);
    }

    @Test
    public void testQueryWithStateAnyOf() {
        Operation operation1 = ImmutableOperation.copyOf(OPERATION_1)
                                                 .withState(State.ERROR);
        Operation operation2 = ImmutableOperation.copyOf(OPERATION_2)
                                                 .withState(State.RUNNING);
        testQueryByCriteria((query, operation) -> query.withStateAnyOf(Arrays.asList(State.ERROR)), operation1, operation2);
    }

    @Test
    public void testQueryInNonFinalState() {
        Operation operation2 = ImmutableOperation.copyOf(OPERATION_2)
                                                 .withState(State.ABORTED);
        testQueryByCriteria((query, operation) -> query.inNonFinalState(), OPERATION_1, operation2);
    }

    @Test
    public void testQueryInFinalState() {
        Operation operation1 = ImmutableOperation.copyOf(OPERATION_1)
                                                 .withState(State.FINISHED);
        testQueryByCriteria((query, operation) -> query.inFinalState(), operation1, OPERATION_2);
    }

    private void testQueryByCriteria(OperationQueryBuilder operationQueryBuilder, Operation operation1, Operation operation2) {
        addOperations(Arrays.asList(operation1, operation2));
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
