package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ProcessTimeCalculator {

    private static final String CALL_ACTIVITY_TYPE = "callActivity";
    private static final String TIMER_EVENT_TYPE = "intermediateCatchEvent";

    private FlowableFacade flowableFacade;
    private LongSupplier currentTimeSupplier;

    public ProcessTimeCalculator(FlowableFacade flowableFacade) {
        this(flowableFacade, System::currentTimeMillis);
    }

    ProcessTimeCalculator(FlowableFacade flowableFacade, LongSupplier currentTimeSupplier) {
        this.flowableFacade = flowableFacade;
        this.currentTimeSupplier = currentTimeSupplier;
    }

    public ProcessTime calculate(String processInstanceId) {
        HistoricProcessInstance rootProcessInstance = flowableFacade.getHistoricProcessById(processInstanceId);

        long processDuration = calculateProcessDuration(rootProcessInstance);

        List<HistoricActivityInstance> processActivities = flowableFacade.getProcessEngine()
                                                                         .getHistoryService()
                                                                         .createHistoricActivityInstanceQuery()
                                                                         .processInstanceId(processInstanceId)
                                                                         .list();
        long processActivitiesTime = calculateFilteredProcessActivitiesTime(processActivities, inst -> true);
        long callActivitiesTime = calculateFilteredProcessActivitiesTime(processActivities, this::isCallActivity);
        long timerEventsTime = calculateFilteredProcessActivitiesTime(processActivities, this::isTimerEvent);

        return ImmutableProcessTime.builder()
                                   .processDuration(processDuration)
                                   .delayBetweenSteps(processDuration - processActivitiesTime + callActivitiesTime + timerEventsTime)
                                   .build();
    }

    private long calculateProcessDuration(HistoricProcessInstance processInstance) {
        Date processInstanceStartTime = processInstance.getStartTime();
        Date processInstanceEndTime = determineProcessInstanceEndTime(processInstance);
        return processInstanceEndTime.getTime() - processInstanceStartTime.getTime();
    }

    private Date determineProcessInstanceEndTime(HistoricProcessInstance processInstance) {
        return processInstance.getEndTime() != null ? processInstance.getEndTime() : new Date(currentTimeSupplier.getAsLong());
    }

    private long calculateFilteredProcessActivitiesTime(List<HistoricActivityInstance> processActivities,
                                                        Predicate<HistoricActivityInstance> filter) {
        return processActivities.stream()
                                .filter(filter)
                                .mapToLong(HistoricActivityInstance::getDurationInMillis)
                                .sum();
    }

    private boolean isCallActivity(HistoricActivityInstance historicActivityInstance) {
        return CALL_ACTIVITY_TYPE.equals(historicActivityInstance.getActivityType());
    }

    private boolean isTimerEvent(HistoricActivityInstance historicActivityInstance) {
        return TIMER_EVENT_TYPE.equals(historicActivityInstance.getActivityType());
    }

    public static class ProcessTimeLogger {
        public static void logProcessTimeIndividually(Logger logger, ProcessTime processTime, String correlationId,
                                                      String processInstanceId) {
            logger.debug(MessageFormat.format("Process time for operation with id \"{0}\", process instance with id \"{1}\", process duration \"{2}\"ms, delay between steps \"{3}\"ms",
                                              correlationId, processInstanceId, processTime.getProcessDuration(),
                                              processTime.getDelayBetweenSteps()));
        }

        public static void logOverallProcessTime(Logger logger, ProcessTime processTime, String correlationId) {
            logger.info(MessageFormat.format("Process time for operation with id \"{0}\", operation duration \"{1}\"ms, delay between steps \"{2}\"ms",
                                             correlationId, processTime.getProcessDuration(), processTime.getDelayBetweenSteps()));
        }
    }

    @Immutable
    @JsonSerialize(as = ImmutableProcessTime.class)
    @JsonDeserialize(as = ImmutableProcessTime.class)
    public interface ProcessTime {
        long getProcessDuration();

        long getDelayBetweenSteps();
    }
}
