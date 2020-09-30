package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class ProcessTimeCalculatorTest {

    private static final String PROCESS_ID = "process-id";

    private final LongSupplier currentTimeSupplier = System::currentTimeMillis;

    private ProcessTimeCalculator processTimeCalculator;

    @Mock
    private FlowableFacade flowableFacade;
    @Mock
    private HistoricProcessInstance mockedProcessInstance;
    @Mock
    private HistoricActivityInstanceQuery historicActivityInstanceQueryMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Mockito.when(flowableFacade.getHistoricProcessById(PROCESS_ID))
               .thenReturn(mockedProcessInstance);
        ProcessEngine processEngine = Mockito.mock(ProcessEngine.class);
        Mockito.when(flowableFacade.getProcessEngine())
               .thenReturn(processEngine);
        HistoryService historyServiceMock = Mockito.mock(HistoryService.class);
        Mockito.when(processEngine.getHistoryService())
               .thenReturn(historyServiceMock);
        Mockito.when(historyServiceMock.createHistoricActivityInstanceQuery())
               .thenReturn(historicActivityInstanceQueryMock);
        Mockito.when(historicActivityInstanceQueryMock.processInstanceId(PROCESS_ID))
               .thenReturn(historicActivityInstanceQueryMock);

        processTimeCalculator = new ProcessTimeCalculator(flowableFacade);
    }

    @Test
    void testProcessDuration() {
        mockProcessStartAndEndTime(5 * 500);

        ProcessTime processTime = processTimeCalculator.calculate(PROCESS_ID);

        assertEquals(5 * 500, processTime.getProcessDuration());
    }

    @Test
    void testAbortedProcessDuration() {
        long currentTime = mockProcessStartTime();
        processTimeCalculator = new ProcessTimeCalculator(flowableFacade, () -> currentTime + 5 * 500);

        ProcessTime processTime = processTimeCalculator.calculate(PROCESS_ID);

        assertEquals(5 * 500, processTime.getProcessDuration());
    }

    static Stream<Arguments> testDelayBetweenSteps() {
        return Stream.of(
       // @formatter:off
                 Arguments.of(0, List.of(Activity.of("serviceTask", 5, 500))),
                 Arguments.of(2000,
                              List.of(Activity.of("exclusiveGateway", 1, 1000), Activity.of("inclusiveGateway", 1, 1000),
                                            Activity.of("serviceTask", 5, 500))),
                 Arguments.of(300, List.of(Activity.of("sequenceFlow", 3, 100), Activity.of("serviceTask", 5, 500))),
                 Arguments.of(500,
                              List.of(Activity.of("intermediateCatchEvent", 5, 100), Activity.of("serviceTask", 5, 500))),
                 Arguments.of(2800,
                              List.of(Activity.of("intermediateCatchEvent", 5, 100), Activity.of("sequenceFlow", 3, 100),
                                            Activity.of("exclusiveGateway", 1, 1000), Activity.of("inclusiveGateway", 1, 1000),
                                            Activity.of("serviceTask", 5, 500)))
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDelayBetweenSteps(long expectedDelayBetweenSteps, List<Activity> activities) {
        mockProcessStartAndEndTime(100); // irrelevant
        List<HistoricActivityInstance> processActivities = mockProcessActivities(activities);
        Mockito.when(historicActivityInstanceQueryMock.list())
               .thenReturn(processActivities);

        ProcessTime processTime = processTimeCalculator.calculate(PROCESS_ID);

        assertEquals(expectedDelayBetweenSteps, processTime.getDelayBetweenSteps());
    }

    @Test
    void testDelayBetweenActivities() {
        mockProcessStartAndEndTime(100); // irrelevant
        long offset = currentTimeSupplier.getAsLong();
        List<HistoricActivityInstance> activities = mockActivitiesWithDelay(offset, Activity.of("serviceTask", 6, 100), 10);
        Mockito.when(historicActivityInstanceQueryMock.list())
               .thenReturn(activities);

        ProcessTime processTime = processTimeCalculator.calculate(PROCESS_ID);

        assertEquals(50, processTime.getDelayBetweenSteps());
    }

    @Test
    void testAbortedProcessDelayBetweenSteps() {
        long currentTime = mockProcessStartTime();
        processTimeCalculator = new ProcessTimeCalculator(flowableFacade, () -> currentTime + 500);

        List<Activity> activities = List.of(Activity.of("serviceTask", 5, 500));
        List<HistoricActivityInstance> processActivities = mockProcessActivities(activities);

        HistoricActivityInstance unfinishedActivity = Mockito.mock(HistoricActivityInstance.class);
        Mockito.when(unfinishedActivity.getActivityType())
               .thenReturn("sequenceFlow");
        Mockito.when(unfinishedActivity.getStartTime())
               .thenReturn(new Date(currentTime));

        processActivities.add(unfinishedActivity);

        Mockito.when(historicActivityInstanceQueryMock.list())
               .thenReturn(processActivities);

        ProcessTime processTime = processTimeCalculator.calculate(PROCESS_ID);

        assertEquals(500, processTime.getDelayBetweenSteps());
    }

    @Test
    void testOverallDelayBetweenSteps() {
        List<Activity> activities = List.of(Activity.of("intermediateCatchEvent", 5, 100), Activity.of("sequenceFlow", 3, 100),
                                            Activity.of("exclusiveGateway", 1, 1000), Activity.of("inclusiveGateway", 1, 1000),
                                            Activity.of("serviceTask", 5, 500));
        long offset = currentTimeSupplier.getAsLong();
        List<HistoricActivityInstance> processActivities = new ArrayList<>();

        for (Activity activity : activities) {
            processActivities.addAll(mockActivitiesWithDelay(offset, activity, 10));

            offset += (activity.numberOf * (activity.duration + 10)) + 10;
        }

        mockProcessStartAndEndTime(100); // irrelevant
        Mockito.when(historicActivityInstanceQueryMock.list())
               .thenReturn(processActivities);

        ProcessTime processTime = processTimeCalculator.calculate(PROCESS_ID);

        assertEquals(2980, processTime.getDelayBetweenSteps());
    }

    private void mockProcessStartAndEndTime(int endTimeDelay) {
        long date = mockProcessStartTime();
        Mockito.when(mockedProcessInstance.getEndTime())
               .thenReturn(new Date(date + endTimeDelay));
    }

    private long mockProcessStartTime() {
        long date = currentTimeSupplier.getAsLong();
        Mockito.when(mockedProcessInstance.getStartTime())
               .thenReturn(new Date(date));
        return date;
    }

    private List<HistoricActivityInstance> mockActivitiesWithoutDelay(long initialOffset, Activity activity) {
        return mockActivitiesWithDelay(initialOffset, activity, 0);
    }

    private List<HistoricActivityInstance> mockActivitiesWithDelay(long initialOffset, Activity activity, long delay) {
        return LongStream.iterate(initialOffset, offset -> offset + activity.duration + delay)
                         .limit(activity.numberOf)
                         .mapToObj(offset -> createActivityMock(offset, activity.type, activity.duration))
                         .collect(Collectors.toList());
    }

    private List<HistoricActivityInstance> mockProcessActivities(List<Activity> activities) {
        long offset = currentTimeSupplier.getAsLong();
        List<HistoricActivityInstance> result = new ArrayList<>();

        for (Activity activity : activities) {
            result.addAll(mockActivitiesWithoutDelay(offset, activity));

            offset += activity.numberOf * activity.duration;
        }
        return result;
    }

    private HistoricActivityInstance createActivityMock(long offset, String activityType, long duration) {
        HistoricActivityInstance activity = Mockito.mock(HistoricActivityInstance.class);
        Mockito.when(activity.getActivityType())
               .thenReturn(activityType);
        Mockito.when(activity.getStartTime())
               .thenReturn(new Date(offset));
        Mockito.when(activity.getEndTime())
               .thenReturn(new Date(offset + duration));
        return activity;
    }

    private static class Activity {
        private String type;
        private long duration;
        private int numberOf;

        static Activity of(String type, int numberOf, long duration) {
            Activity activity = new Activity();
            activity.type = type;
            activity.numberOf = numberOf;
            activity.duration = duration;
            return activity;
        }
    }
}