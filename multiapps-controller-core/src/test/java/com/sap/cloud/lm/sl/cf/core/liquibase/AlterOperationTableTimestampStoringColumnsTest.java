package com.sap.cloud.lm.sl.cf.core.liquibase;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.liquibase.AlterOperationTableTimestampStoringColumnsPostgresqlChange.OriginalOperation;
import com.sap.cloud.lm.sl.cf.core.liquibase.AlterOperationTableTimestampStoringColumnsPostgresqlChange.TransformedOperation;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class AlterOperationTableTimestampStoringColumnsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private AlterOperationTableTimestampStoringColumnsPostgresqlChange change = new AlterOperationTableTimestampStoringColumnsPostgresqlChange();
    private List<OriginalOperation> originalOperations;
    private List<ExpectedOperation> expectedOperations;
    private String originalOperationsJsonLocation;
    private String expectedOperationsJsonLocation;
    private Class<? extends Throwable> expectedExceptionType;
    public AlterOperationTableTimestampStoringColumnsTest(String originalOperationsJsonLocation, String expectedOperationsJsonLocation,
                                                          Class<? extends Exception> expectedExceptionType) {
        this.originalOperationsJsonLocation = originalOperationsJsonLocation;
        this.expectedOperationsJsonLocation = expectedOperationsJsonLocation;
        this.expectedExceptionType = expectedExceptionType;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0)
            {
                "original-operations.json", "expected-operations.json", null,
            },
            // (1)
            {
                "original-operations-without-start-and-end-times.json", "transformed-operations-without-start-and-end-times.json", null,
            },
            // (2)
            {
                "original-operations-with-start-and-end-times-in-an-unknown-format.json", null, DateTimeParseException.class,
            },
// @formatter:on
        });
    }

    public static void main(String[] args) {
        System.out.println(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()));
    }

    @Before
    public void loadOriginalOperations() throws Exception {
        String originalOperationsJson = TestUtil.getResourceAsString(originalOperationsJsonLocation, getClass());
        this.originalOperations = JsonUtil.fromJson(originalOperationsJson, new TypeToken<List<OriginalOperation>>() {
        }.getType());
    }

    @Before
    public void loadExpectedOperations() throws Exception {
        if (expectedOperationsJsonLocation == null) {
            return;
        }
        String expectedOperationsJson = TestUtil.getResourceAsString(expectedOperationsJsonLocation, getClass());
        this.expectedOperations = JsonUtil.fromJson(expectedOperationsJson, new TypeToken<List<ExpectedOperation>>() {
        }.getType());
    }

    @Before
    public void expectException() {
        if (expectedExceptionType != null) {
            expectedException.expect(expectedExceptionType);
        }
    }

    @Test
    public void testTransformData() {
        List<TransformedOperation> transformedOperations = change.transformData(originalOperations);

        assertEquals(expectedOperations.size(), transformedOperations.size());

        for (int index = 0; index < expectedOperations.size(); index++) {
            validateTransformedOperation(expectedOperations.get(index), transformedOperations.get(index));
        }
    }

    private void validateTransformedOperation(ExpectedOperation expectedOperation, TransformedOperation transformedOperation) {
        assertEquals(expectedOperation.processId, transformedOperation.processId);
        assertEquals(expectedOperation.startedAtInEpochMillis, toEpochMillis(transformedOperation.startedAt));
        assertEquals(expectedOperation.endedAtInEpochMillis, toEpochMillis(transformedOperation.endedAt));
    }

    private Long toEpochMillis(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.getTime();
    }

    static class ExpectedOperation {

        String processId;
        Long startedAtInEpochMillis;
        Long endedAtInEpochMillis;

    }

}
