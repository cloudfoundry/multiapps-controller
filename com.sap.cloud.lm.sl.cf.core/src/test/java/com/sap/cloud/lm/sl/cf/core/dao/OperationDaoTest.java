package com.sap.cloud.lm.sl.cf.core.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.core.helpers.OperationFactory;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;

public class OperationDaoTest {

    private static final String OPERATION_1_ID = "1";
    private static final String OPERATION_2_ID = "2";
    private static final Operation OPERATION_1 = createOperation(OPERATION_1_ID);
    private static final Operation OPERATION_2 = createOperation(OPERATION_2_ID);

    private OperationDao dao = createDao();

    @AfterEach
    public void clearDatabase() {
        for (Operation operation : dao.findAll()) {
            dao.remove(operation.getProcessId());
        }
    }

    @Test
    public void testAdd() {
        dao.add(OPERATION_1);

        Set<Operation> operations = asSet(dao.findAll());

        Set<Operation> expectedOperations = asSet(OPERATION_1);
        assertEquals(expectedOperations, operations);
    }

    @Test
    public void testAddWithNonEmptyDatabase() {
        dao.add(OPERATION_1);
        dao.add(OPERATION_2);

        Set<Operation> operations = asSet(dao.findAll());

        Set<Operation> expectedOperations = asSet(OPERATION_1, OPERATION_2);
        assertEquals(expectedOperations, operations);
    }

    @Test
    public void testAddWithAlreadyExistingOperation() {
        dao.add(OPERATION_1);

        Exception exception = assertThrows(ConflictException.class, () -> dao.add(OPERATION_1));

        String expectedExceptionMessage = MessageFormat.format(Messages.OPERATION_ALREADY_EXISTS, OPERATION_1_ID);
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void testRemove() {
        dao.add(OPERATION_1);
        assertEquals(1, dao.findAll()
            .size());

        dao.remove(OPERATION_1_ID);

        assertEquals(0, dao.findAll()
            .size());
    }

    @Test
    public void testRemoveWithNonExistingOperation() {
        Exception exception = assertThrows(NotFoundException.class, () -> dao.remove(OPERATION_1_ID));

        String expectedExceptionMessage = MessageFormat.format(Messages.OPERATION_NOT_FOUND, OPERATION_1_ID);
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void testFindRequired() {
        dao.add(OPERATION_1);
        Operation retrievedOperation = dao.findRequired(OPERATION_1_ID);
        assertEquals(OPERATION_1, retrievedOperation);
    }

    @Test
    public void testFindRequiredWithNonExistingOperation() {
        Exception exception = assertThrows(NotFoundException.class, () -> dao.findRequired(OPERATION_1_ID));

        String expectedExceptionMessage = MessageFormat.format(Messages.OPERATION_NOT_FOUND, OPERATION_1_ID);
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void testFindWithNonExistingOperation() {
        assertNull(dao.find(OPERATION_1_ID));
    }

    @Test
    public void testFilteringWithEndedBefore() {
        Operation operation1 = createOperation("1").endedAt(ZonedDateTime.parse("2017-11-18T22:00:00.000Z[UTC]"));
        Operation operation2 = createOperation("2").endedAt(ZonedDateTime.parse("2017-11-19T15:00:00.000Z[UTC]"));
        List<Operation> databaseContent = Arrays.asList(operation1, operation2);

        OperationFilter filter = new OperationFilter.Builder().endedBefore(parseDate("2017-11-19T10:00:00.000Z[UTC]"))
            .build();

        List<Operation> expectedResult = Arrays.asList(operation1);
        testFiltering(databaseContent, filter, expectedResult);
    }

    @Test
    public void testFilteringWithEndedAfter() {
        Operation operation1 = createOperation("1").endedAt(ZonedDateTime.parse("2017-11-18T22:00:00.000Z[UTC]"));
        Operation operation2 = createOperation("2").endedAt(ZonedDateTime.parse("2017-11-19T15:00:00.000Z[UTC]"));
        List<Operation> databaseContent = Arrays.asList(operation1, operation2);

        OperationFilter filter = new OperationFilter.Builder().endedAfter(parseDate("2017-11-19T10:00:00.000Z[UTC]"))
            .build();

        List<Operation> expectedResult = Arrays.asList(operation2);
        testFiltering(databaseContent, filter, expectedResult);
    }

    @Test
    public void testFilteringWithEndedBeforeAndEndedAfter() {
        Operation operation1 = createOperation("1").endedAt(ZonedDateTime.parse("2017-11-18T22:00:00.000Z[UTC]"));
        Operation operation2 = createOperation("2").endedAt(ZonedDateTime.parse("2017-11-19T15:00:00.000Z[UTC]"));
        Operation operation3 = createOperation("3").endedAt(ZonedDateTime.parse("2017-11-19T19:00:00.000Z[UTC]"));
        List<Operation> databaseContent = Arrays.asList(operation1, operation2, operation3);

        OperationFilter filter = new OperationFilter.Builder().endedAfter(parseDate("2017-11-19T10:00:00.000Z[UTC]"))
            .endedBefore(parseDate("2017-11-19T19:00:00.000Z[UTC]"))
            .build();

        List<Operation> expectedResult = Arrays.asList(operation2);
        testFiltering(databaseContent, filter, expectedResult);
    }

    @Test
    public void testFilteringWithSpaceId() {
        Operation operation1 = createOperation("1").spaceId("c65d042c-324f-4dc5-a925-9c806acafcfb");
        Operation operation2 = createOperation("2").spaceId("9818394d-38d6-47a6-b080-1ba013a4932c");
        List<Operation> databaseContent = Arrays.asList(operation1, operation2);

        OperationFilter filter = new OperationFilter.Builder().spaceId("c65d042c-324f-4dc5-a925-9c806acafcfb")
            .build();

        List<Operation> expectedResult = Arrays.asList(operation1);
        testFiltering(databaseContent, filter, expectedResult);
    }

    @Test
    public void testFilteringWithUsername() {
        Operation operation1 = createOperation("1").user("nictas");
        Operation operation2 = createOperation("2").user("doruug");
        List<Operation> databaseContent = Arrays.asList(operation1, operation2);

        OperationFilter filter = new OperationFilter.Builder().user("nictas")
            .build();

        List<Operation> expectedResult = Arrays.asList(operation1);
        testFiltering(databaseContent, filter, expectedResult);
    }

    @Test
    public void testFilteringWithMtaId() {
        Operation operation1 = createOperation("1").mtaId("anatz");
        Operation operation2 = createOperation("2").mtaId("denip");
        List<Operation> databaseContent = Arrays.asList(operation1, operation2);

        OperationFilter filter = new OperationFilter.Builder().mtaId("anatz")
            .build();

        List<Operation> expectedResult = Arrays.asList(operation1);
        testFiltering(databaseContent, filter, expectedResult);
    }

    @Test
    public void testFilteringOfNonFinalOperations() {
        Operation operation1 = createOperation("1");
        Operation operation2 = createOperation("2").state(State.ABORTED);
        Operation operation3 = createOperation("3");
        List<Operation> databaseContent = Arrays.asList(operation1, operation2, operation3);

        OperationFilter filter = new OperationFilter.Builder().inNonFinalState()
            .build();

        List<Operation> expectedResult = Arrays.asList(operation1, operation3);
        testFiltering(databaseContent, filter, expectedResult);
    }

    @Test
    public void testFilteringOfFinalOperations() {
        Operation operation1 = createOperation("1").state(State.FINISHED);
        Operation operation2 = createOperation("2");
        Operation operation3 = createOperation("3").state(State.ABORTED);
        Operation operation4 = createOperation("4").state(State.ABORTED);
        List<Operation> databaseContent = Arrays.asList(operation1, operation2, operation3, operation4);

        OperationFilter filter = new OperationFilter.Builder().inFinalState()
            .build();

        List<Operation> expectedResult = Arrays.asList(operation1, operation3, operation4);
        testFiltering(databaseContent, filter, expectedResult);
    }

    @Test
    public void testLimitingNumberOfResults() {
        Operation operation1 = createOperation("1");
        Operation operation2 = createOperation("2");
        Operation operation3 = createOperation("3");
        List<Operation> databaseContent = Arrays.asList(operation1, operation2, operation3);

        OperationFilter filter = new OperationFilter.Builder().maxResults(2)
            .build();

        List<Operation> expectedResult = Arrays.asList(operation1, operation2);
        testFiltering(databaseContent, filter, expectedResult);
    }

    @Test
    public void testPaging() {
        Operation operation1 = createOperation("1");
        Operation operation2 = createOperation("2");
        Operation operation3 = createOperation("3");
        Operation operation4 = createOperation("4");
        Operation operation5 = createOperation("5");
        List<Operation> databaseContent = Arrays.asList(operation1, operation2, operation3, operation4, operation5);

        OperationFilter filter = new OperationFilter.Builder().maxResults(2)
            .firstElement(2)
            .build();

        List<Operation> expectedResult = Arrays.asList(operation3, operation4);
        testFiltering(databaseContent, filter, expectedResult);
    }

    @Test
    public void testPagingWithIncompletePage() {
        Operation operation1 = createOperation("1");
        Operation operation2 = createOperation("2");
        Operation operation3 = createOperation("3");
        List<Operation> databaseContent = Arrays.asList(operation1, operation2, operation3);

        OperationFilter filter = new OperationFilter.Builder().maxResults(2)
            .firstElement(2)
            .build();

        List<Operation> expectedResult = Arrays.asList(operation3);
        testFiltering(databaseContent, filter, expectedResult);
    }

    @Test
    public void testPagingWhenFirstElementIsOutOfBounds() {
        List<Operation> databaseContent = Collections.emptyList();

        OperationFilter filter = new OperationFilter.Builder().maxResults(2)
            .firstElement(2)
            .build();

        List<Operation> expectedResult = Collections.emptyList();
        testFiltering(databaseContent, filter, expectedResult);
    }

    private void testFiltering(List<Operation> databaseContent, OperationFilter filter, List<Operation> expectedResult) {
        addOperations(databaseContent);
        List<Operation> result = dao.find(filter);
        assertEquals(expectedResult, result);
    }

    private void addOperations(List<Operation> operations) {
        for (Operation operation : operations) {
            dao.add(operation);
        }
    }

    private static OperationDao createDao() {
        OperationDao dao = new OperationDao();
        OperationDtoDao dtoDao = new OperationDtoDao();
        dtoDao.emf = Persistence.createEntityManagerFactory("OperationManagement");
        dao.dao = dtoDao;
        dao.operationFactory = new OperationFactory();
        return dao;
    }

    private static Operation createOperation(String id) {
        return new Operation().processId(id)
            .acquiredLock(false);
    }

    private static Date parseDate(String date) {
        return Date.from(ZonedDateTime.parse(date)
            .toInstant());
    }

    private static Set<Operation> asSet(Operation... operations) {
        return asSet(Arrays.asList(operations));
    }

    private static Set<Operation> asSet(List<Operation> operations) {
        return new HashSet<>(operations);
    }

}
