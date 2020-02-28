package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.ListUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTimeCalculator.ProcessTime;

public class ProcessTimeCalculatorTest {

    private static final String DEFAULT_PROCESS_ID = "process-id-wich-is-too-long-in-order-to-be-like-uuid";
    private static final String DEFAULT_ACTIVITY_ID_PREFIX = "activityIdPrefix_";

    private ProcessTimeCalculator processTimeCalculator;

    private Supplier<Long> currentTimeSupplier = System::currentTimeMillis;

    @Mock
    private FlowableFacade flowableFacade;

    @Mock
    private HistoricProcessInstance mockedProcessInstance;

    @Mock
    private HistoricActivityInstanceQuery historicActivityInstanceQueryMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(flowableFacade.getHistoricProcessById(DEFAULT_PROCESS_ID))
               .thenReturn(mockedProcessInstance);
        ProcessEngine processEngine = Mockito.mock(ProcessEngine.class);
        Mockito.when(flowableFacade.getProcessEngine())
               .thenReturn(processEngine);
        HistoryService historyServiceMock = Mockito.mock(HistoryService.class);
        Mockito.when(processEngine.getHistoryService())
               .thenReturn(historyServiceMock);
        Mockito.when(historyServiceMock.createHistoricActivityInstanceQuery())
               .thenReturn(historicActivityInstanceQueryMock);
        Mockito.when(historicActivityInstanceQueryMock.processInstanceId(DEFAULT_PROCESS_ID))
               .thenReturn(historicActivityInstanceQueryMock);

        processTimeCalculator = new ProcessTimeCalculator(flowableFacade);
    }

    @Test
    public void testWithValidProcessWithNoCallActivitiesShouldReturnNoDelayBetweenSteps() {
        mockProcessStartAndEndTime(5 * 500);
        List<HistoricActivityInstance> processActivities = mockProcessActivities("serviceTask", 5, 500);
        Mockito.when(historicActivityInstanceQueryMock.list())
               .thenReturn(processActivities);
        ProcessTime processTime = processTimeCalculator.calculate(DEFAULT_PROCESS_ID);
        Assertions.assertEquals(0, processTime.getDelayBetweenSteps());
    }

    @Test
    public void testWithValidProcessWithOneCallActivity() {
        mockProcessStartAndEndTime(5 * 500 + 1500);
        List<HistoricActivityInstance> callActivities = mockProcessActivities("callActivity", 1, 1500);
        List<HistoricActivityInstance> processActivities = mockProcessActivities("serviceTask", 5, 500);
        Mockito.when(historicActivityInstanceQueryMock.list())
               .thenReturn(ListUtils.union(callActivities, processActivities));
        ProcessTime processTime = processTimeCalculator.calculate(DEFAULT_PROCESS_ID);
        Assertions.assertEquals(1500, processTime.getDelayBetweenSteps());
    }

    @Test
    public void testWithValidProcessWithTwoCallActivities() {
        mockProcessStartAndEndTime(5 * 500 + 1500);
        List<HistoricActivityInstance> callActivities = mockProcessActivities("callActivity", 2, 1500);
        List<HistoricActivityInstance> processActivities = mockProcessActivities("serviceTask", 5, 500);
        Mockito.when(historicActivityInstanceQueryMock.list())
               .thenReturn(ListUtils.union(callActivities, processActivities));
        ProcessTime processTime = processTimeCalculator.calculate(DEFAULT_PROCESS_ID);
        Assertions.assertEquals(1500, processTime.getDelayBetweenSteps());
    }

    @Test
    public void testWithNoEndTime() {
        Long currentTime = mockProcessStartTime();
        processTimeCalculator = new ProcessTimeCalculator(flowableFacade, () -> currentTime + 5 * 500);
        List<HistoricActivityInstance> processActivities = mockProcessActivities("serviceTask", 5, 500);
        Mockito.when(historicActivityInstanceQueryMock.list())
               .thenReturn(processActivities);
        ProcessTime processTime = processTimeCalculator.calculate(DEFAULT_PROCESS_ID);
        Assertions.assertEquals(0, processTime.getDelayBetweenSteps());
    }

    @Test
    public void testWithHugeDelayBetweenSteps() {
        mockProcessStartAndEndTime(5 * 500 + 10000);
        List<HistoricActivityInstance> processActivities = mockProcessActivities("serviceTask", 5, 500);
        Mockito.when(historicActivityInstanceQueryMock.list())
               .thenReturn(processActivities);
        ProcessTime processTime = processTimeCalculator.calculate(DEFAULT_PROCESS_ID);
        Assertions.assertEquals(10000, processTime.getDelayBetweenSteps());
    }

    private void mockProcessStartAndEndTime(int endTimeDelay) {
        Long date = mockProcessStartTime();
        Mockito.when(mockedProcessInstance.getEndTime())
               .thenReturn(new Date(date + endTimeDelay));
    }

    private Long mockProcessStartTime() {
        Long date = currentTimeSupplier.get();
        Mockito.when(mockedProcessInstance.getStartTime())
               .thenReturn(new Date(date));
        return date;
    }

    private List<HistoricActivityInstance> mockProcessActivities(String activityType, int numberOfActivities, long durationIntervalMilis) {
        return IntStream.range(0, numberOfActivities)
                        .mapToObj(currentActivityId -> createActivityMock(activityType, durationIntervalMilis, currentActivityId))
                        .collect(Collectors.toList());

    }

    private HistoricActivityInstance createActivityMock(String activityType, long durationIntervalMilis, int currentActivityId) {
        HistoricActivityInstance activity = Mockito.mock(HistoricActivityInstance.class);
        Mockito.when(activity.getActivityId())
               .thenReturn(DEFAULT_ACTIVITY_ID_PREFIX + currentActivityId);
        Mockito.when(activity.getActivityType())
               .thenReturn(activityType);
        Mockito.when(activity.getDurationInMillis())
               .thenReturn(durationIntervalMilis);
        return activity;
    }

}