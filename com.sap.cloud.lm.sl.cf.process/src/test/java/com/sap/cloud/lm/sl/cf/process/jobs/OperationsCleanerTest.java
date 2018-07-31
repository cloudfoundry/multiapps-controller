package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

public class OperationsCleanerTest {

    protected ZonedDateTime epochMillisToZonedDateTime(long epochMillis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    protected void mockOperationDao(OperationDao dao, List<Operation> operationsList) {
        when(dao.find((OperationFilter) any())).thenAnswer(new Answer<List<Operation>>() {
            @Override
            public List<Operation> answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                OperationFilter filter = (OperationFilter) args[0];

                List<Operation> result = operationsList.stream()
                    .filter(operation -> filterOperations(operation, filter))
                    .collect(Collectors.toList());

                return result;
            }
        });

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Operation operation = (Operation) args[0];
                assertTrue("OperationDao should only merge cleaned up operations!", operation.isCleanedUp());
                return null;
            }
        }).when(dao)
            .merge(any());
    }

    private boolean filterOperations(Operation operation, OperationFilter filter) {
        if (operation.isCleanedUp() != null && filter.isCleanedUp() != operation.isCleanedUp()) {
            return false;
        }

        if (filter.isInNonFinalState() && !State.getActiveStates()
            .contains(operation.getState())) {
            return false;
        }

        Instant beforeDateInstant = filter.getStartTimeUpperBound()
            .toInstant();
        Instant startedAtInstant = operation.getStartedAt()
            .toInstant();

        return beforeDateInstant.isAfter(startedAtInstant);
    }

}
