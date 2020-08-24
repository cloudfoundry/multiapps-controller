package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;

@Named
public class OperationTimeAggregator {

    private final ProcessTimeCalculator processTimeCalculator;
    private final FlowableFacade flowableFacade;

    @Inject
    public OperationTimeAggregator(FlowableFacade flowableFacade) {
        this.flowableFacade = flowableFacade;
        this.processTimeCalculator = new ProcessTimeCalculator(flowableFacade);
    }

    public Map<String, ProcessTime> collectProcessTimes(String correlationId) {
        List<String> historicSubProcesses = flowableFacade.getHistoricSubProcessIds(correlationId);
        historicSubProcesses.add(correlationId);

        return historicSubProcesses.stream()
                                   .collect(Collectors.toMap(processId -> processId, processTimeCalculator::calculate));
    }

    public ProcessTime computeOverallProcessTime(String correlationId, Map<String, ProcessTime> processTimes) {
        ProcessTime rootProcessTime = processTimes.get(correlationId);

        long overallDelayBetweenSteps = processTimes.values()
                                                    .stream()
                                                    .mapToLong(ProcessTime::getDelayBetweenSteps)
                                                    .sum();

        return ImmutableProcessTime.copyOf(rootProcessTime)
                                   .withDelayBetweenSteps(overallDelayBetweenSteps);
    }
}
