package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OperationTimeAggregatorTest {

    private static final String CORRELATION_ID = "id";

    private OperationTimeAggregator operationTimeAggregator;

    @BeforeEach
    void setUp() {
        FlowableFacade flowableFacade = Mockito.mock(FlowableFacade.class);
        operationTimeAggregator = new OperationTimeAggregator(flowableFacade);
    }

    @Test
    void testOperationTimeWithNoSubProcesses() {
        Map<String, ProcessTime> processTimes = new HashMap<>();
        processTimes.put(CORRELATION_ID, createProcessTime(100, 200));

        ProcessTime overallProcessTime = operationTimeAggregator.computeOverallProcessTime(CORRELATION_ID, processTimes);

        assertEquals(100, overallProcessTime.getProcessDuration());
        assertEquals(200, overallProcessTime.getDelayBetweenSteps());
    }

    @Test
    void testOperationTimeWithInvalidInput() {
        assertThrows(NullPointerException.class,
                     () -> operationTimeAggregator.computeOverallProcessTime(CORRELATION_ID, Collections.emptyMap()));
    }

    @Test
    void testOperationTimeWithOneSubProcess() {
        Map<String, ProcessTime> processTimes = new HashMap<>();
        processTimes.put(CORRELATION_ID, createProcessTime(100, 200));
        processTimes.put("id-1", createProcessTime(50, 100));

        ProcessTime overallProcessTime = operationTimeAggregator.computeOverallProcessTime(CORRELATION_ID, processTimes);

        assertEquals(100, overallProcessTime.getProcessDuration());
        assertEquals(300, overallProcessTime.getDelayBetweenSteps());
    }

    @Test
    void testOperationTimeWithTwoSubProcesses() {
        Map<String, ProcessTime> processTimes = new HashMap<>();
        processTimes.put(CORRELATION_ID, createProcessTime(100, 200));
        processTimes.put("id-1", createProcessTime(50, 100));
        processTimes.put("id-2", createProcessTime(70, 300));

        ProcessTime overallProcessTime = operationTimeAggregator.computeOverallProcessTime(CORRELATION_ID, processTimes);

        assertEquals(100, overallProcessTime.getProcessDuration());
        assertEquals(600, overallProcessTime.getDelayBetweenSteps());
    }

    private ProcessTime createProcessTime(long duration, long delayBetweenSteps) {
        return ImmutableProcessTime.builder()
                                   .processDuration(duration)
                                   .delayBetweenSteps(delayBetweenSteps)
                                   .build();
    }
}
