package com.sap.cloud.lm.sl.cf.core.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;

public class OperationDaoTest extends AbstractOperationDaoParameterizedTest {

    private static final String OPERATION_1_ID = "1";
    private static final String OPERATION_2_ID = "2";
    private static final Operation OPERATION_1 = createOperation(OPERATION_1_ID);
    private static final Operation OPERATION_2 = createOperation(OPERATION_2_ID);

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

    private static Set<Operation> asSet(Operation... operations) {
        return asSet(Arrays.asList(operations));
    }

    private static Set<Operation> asSet(List<Operation> operations) {
        return new HashSet<>(operations);
    }

    private static Operation createOperation(String id) {
        return new Operation().processId(id)
            .acquiredLock(false);
    }

}
