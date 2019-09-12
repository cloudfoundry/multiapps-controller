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
        testQueryByCriteria((query, operation) -> query.processId(operation.getProcessId()));
    }

    @Test
    public void testQueryByProcessType() {
        testQueryByCriteria((query, operation) -> query.processType(operation.getProcessType()));
    }

    @Test
    public void testQueryBySpaceId() {
        testQueryByCriteria((query, operation) -> query.spaceId(operation.getSpaceId()));
    }

    @Test
    public void testQueryByMtaId() {
        testQueryByCriteria((query, operation) -> query.mtaId(operation.getMtaId()));
    }

    @Test
    public void testQueryByUser() {
        testQueryByCriteria((query, operation) -> query.user(operation.getUser()));
    }

    @Test
    public void testQueryByAcquiredLock() {
        testQueryByCriteria((query, operation) -> query.acquiredLock(operation.hasAcquiredLock()));
    }

    @Test
    public void testQueryByState() {
        OPERATION_1.state(State.ERROR);
        OPERATION_2.state(State.RUNNING);
        testQueryByCriteria((query, operation) -> query.state(operation.getState()));
    }

    @Test
    public void testQueryByStartedBefore() {
        testQueryByCriteria((query, operation) -> query.startedBefore(toDate(ZonedDateTime.parse("2010-10-09T00:00:00.000Z[UTC]"))));
    }

    @Test
    public void testQueryByEndedAfter() {
        testQueryByCriteria((query, operation) -> query.endedAfter(toDate(ZonedDateTime.parse("2010-10-13T10:00:00.000Z[UTC]"))));
    }

    @Test
    public void testQueryWithStateAnyOf() {
        OPERATION_1.state(State.ERROR);
        OPERATION_2.state(State.RUNNING);
        testQueryByCriteria((query, operation) -> query.withStateAnyOf(Arrays.asList(State.ERROR)));
    }

    @Test
    public void testQueryInNonFinalState() {
        OPERATION_1.state(null);
        OPERATION_2.state(State.ABORTED);
        testQueryByCriteria((query, operation) -> query.inNonFinalState());
    }

    @Test
    public void testQueryInFinalState() {
        OPERATION_1.state(State.FINISHED);
        OPERATION_2.state(null);
        testQueryByCriteria((query, operation) -> query.inFinalState());
    }

    private void testQueryByCriteria(OperationQueryBuilder operationQueryBuilder) {
        addOperations(Arrays.asList(OPERATION_1, OPERATION_2));
        assertEquals(1, operationQueryBuilder.build(operationService.createQuery(), OPERATION_1)
                                             .list()
                                             .size());
        assertEquals(1, operationQueryBuilder.build(operationService.createQuery(), OPERATION_1)
                                             .delete());
        assertOperationExists(OPERATION_2.getProcessId());
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
        return new Operation().processId(processId)
                              .processType(type)
                              .spaceId(spaceId)
                              .mtaId(mtaId)
                              .user(user)
                              .acquiredLock(acquiredLock)
                              .startedAt(startedAt)
                              .endedAt(endedAt);
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
