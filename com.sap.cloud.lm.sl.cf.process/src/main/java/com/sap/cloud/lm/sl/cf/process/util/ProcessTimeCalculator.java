package com.sap.cloud.lm.sl.cf.process.util;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;

public class ProcessTimeCalculator {

    private static final String CALL_ACTIVITY_TYPE = "callActivity";
    private static final String TIMER_EVENT_TYPE = "intermediateCatchEvent";

    private FlowableFacade flowableFacade;
    private Supplier<Long> currentTimeSupplier;

    public ProcessTimeCalculator(FlowableFacade flowableFacade) {
        this(flowableFacade, System::currentTimeMillis);
    }

    ProcessTimeCalculator(FlowableFacade flowableFacade, Supplier<Long> currentTimeSupplier) {
        this.flowableFacade = flowableFacade;
        this.currentTimeSupplier = currentTimeSupplier;
    }

    public ProcessTime calculate(String processInstanceId) {
        HistoricProcessInstance rootProcessInstance = flowableFacade.getHistoricProcessById(processInstanceId);

        long processDuration = calculateProcessDuration(rootProcessInstance);
        long allProcessStepsDuration = calculateAllProcessStepsDuration(processInstanceId);
        return ImmutableProcessTime.builder()
                                   .processDuration(processDuration)
                                   .delayBetweenSteps(processDuration - allProcessStepsDuration)
                                   .build();
    }

    private long calculateProcessDuration(HistoricProcessInstance processInstance) {
        Date processInstanceStartTime = processInstance.getStartTime();
        Date processInstanceEndTime = determineProcessInstanceEndTime(processInstance);
        return processInstanceEndTime.getTime() - processInstanceStartTime.getTime();
    }

    private Date determineProcessInstanceEndTime(HistoricProcessInstance processInstance) {
        return processInstance.getEndTime() != null ? processInstance.getEndTime() : new Date(currentTimeSupplier.get());
    }

    private long calculateAllProcessStepsDuration(String processInstanceId) {
        List<HistoricActivityInstance> processActivities = flowableFacade.getProcessEngine()
                                                                         .getHistoryService()
                                                                         .createHistoricActivityInstanceQuery()
                                                                         .processInstanceId(processInstanceId)
                                                                         .list();
        return processActivities.stream()
                                .filter(this::isNotCallActivity)
                                .filter(this::isNotTimerEvent)
                                .mapToLong(HistoricActivityInstance::getDurationInMillis)
                                .sum();
    }

    private boolean isNotCallActivity(HistoricActivityInstance historicActivityInstance) {
        return !CALL_ACTIVITY_TYPE.equals(historicActivityInstance.getActivityType());
    }

    private boolean isNotTimerEvent(HistoricActivityInstance historicActivityInstance) {
        return !TIMER_EVENT_TYPE.equals(historicActivityInstance.getActivityType());
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
