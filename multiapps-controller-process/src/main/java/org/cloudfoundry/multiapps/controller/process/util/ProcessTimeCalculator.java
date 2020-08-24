package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;

public class ProcessTimeCalculator {

    private static final String TIMER_EVENT_TYPE = "intermediateCatchEvent";
    private static final String SEQUENCE_FLOW_TYPE = "sequenceFlow";
    private static final String GATEWAY_TYPE = "Gateway";

    private final FlowableFacade flowableFacade;
    private final LongSupplier currentTimeSupplier;

    public ProcessTimeCalculator(FlowableFacade flowableFacade) {
        this(flowableFacade, System::currentTimeMillis);
    }

    ProcessTimeCalculator(FlowableFacade flowableFacade, LongSupplier currentTimeSupplier) {
        this.flowableFacade = flowableFacade;
        this.currentTimeSupplier = currentTimeSupplier;
    }

    public ProcessTime calculate(String processInstanceId) {
        HistoricProcessInstance rootProcessInstance = flowableFacade.getHistoricProcessById(processInstanceId);
        List<HistoricActivityInstance> processActivities = flowableFacade.getProcessEngine()
                                                                         .getHistoryService()
                                                                         .createHistoricActivityInstanceQuery()
                                                                         .processInstanceId(processInstanceId)
                                                                         .list();
        long processDuration = calculateProcessDuration(rootProcessInstance);

        long sequenceFlowsTime = calculateFilteredProcessActivitiesTime(processActivities, this::isSequenceFlow);
        long timerEventsTime = calculateFilteredProcessActivitiesTime(processActivities, this::isTimerEvent);
        long gatewaysTime = calculateFilteredProcessActivitiesTime(processActivities, this::isGateway);
        long delayBetweenActivities = getDelayBetweenActivities(processActivities);

        return ImmutableProcessTime.builder()
                                   .processDuration(processDuration)
                                   .delayBetweenSteps(sequenceFlowsTime + timerEventsTime + gatewaysTime + delayBetweenActivities)
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
                                .mapToLong(this::calculateActivityDuration)
                                .sum();
    }

    private long calculateActivityDuration(HistoricActivityInstance activityInstance) {
        Date startTime = activityInstance.getStartTime();
        Date endTime = determineProcessActivityEndTime(activityInstance);
        return endTime.getTime() - startTime.getTime();
    }

    private Date determineProcessActivityEndTime(HistoricActivityInstance activityInstance) {
        return activityInstance.getEndTime() == null ? new Date(currentTimeSupplier.getAsLong()) : activityInstance.getEndTime();
    }

    private boolean isTimerEvent(HistoricActivityInstance historicActivityInstance) {
        return TIMER_EVENT_TYPE.equals(historicActivityInstance.getActivityType());
    }

    private boolean isSequenceFlow(HistoricActivityInstance historicActivityInstance) {
        return SEQUENCE_FLOW_TYPE.equals(historicActivityInstance.getActivityType());
    }

    private boolean isGateway(HistoricActivityInstance historicActivityInstance) {
        String activityType = historicActivityInstance.getActivityType();
        return activityType != null && activityType.endsWith(GATEWAY_TYPE);
    }

    private long getDelayBetweenActivities(List<HistoricActivityInstance> activities) {
        HistoricActivityInstance[] processActivities = activities.toArray(new HistoricActivityInstance[0]);
        Arrays.sort(processActivities, Comparator.comparing(HistoricActivityInstance::getStartTime));
        long result = 0;

        for (int i = 0; i < processActivities.length - 1; i++) {
            Date nextActivityStartTime = processActivities[i + 1].getStartTime();
            Date currentActivityEndTime = determineProcessActivityEndTime(processActivities[i]);

            long delay = nextActivityStartTime.getTime() - currentActivityEndTime.getTime();
            //if the delay is negative, the activities are parallel
            //whereas we want the delay as if the process is sequential
            result += delay < 0 ? 0 : delay;
        }
        return result;
    }
}
