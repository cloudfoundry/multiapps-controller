package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTimeCalculator.ProcessTime;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTimeCalculator.ProcessTimeLogger;

@Named
public class OperationTimeAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationTimeAggregator.class);

    private ProcessTimeCalculator processTimeCalculator;
    private FlowableFacade flowableFacade;

    @Inject
    public OperationTimeAggregator(FlowableFacade flowableFacade) {
        this.flowableFacade = flowableFacade;
        this.processTimeCalculator = new ProcessTimeCalculator(flowableFacade);
    }

    public void aggregateOperationTime(String correlationId) {
        List<String> historicSubProcesses = flowableFacade.getHistoricSubProcessIds(correlationId);
        historicSubProcesses.add(correlationId);

        Map<String, ProcessTime> processTimesForSubProcesses = historicSubProcesses.stream()
                                                                                   .collect(Collectors.toMap(processId -> processId,
                                                                                                             processTimeCalculator::calculate));
        processTimesForSubProcesses.forEach((key, value) -> logProcessTimeIndividually(value, correlationId,
                key));

        ProcessTime rootProcessTime = processTimesForSubProcesses.get(correlationId);
        logOverallProcesstime(correlationId, rootProcessTime, processTimesForSubProcesses.values());
    }

    private void logProcessTimeIndividually(ProcessTime processTime, String correlationId, String processInstanceId) {
        ProcessTimeLogger.logProcessTimeIndividually(LOGGER, processTime, correlationId, processInstanceId);
    }

    private void logOverallProcesstime(String correlationId, ProcessTime rootProcessTime,
                                       Collection<ProcessTime> subProcessesProcessTimes) {
        long overallDelayBetweenSteps = subProcessesProcessTimes.stream()
                                                                .mapToLong(ProcessTime::getDelayBetweenSteps)
                                                                .sum();
        ProcessTime overallProcessTime = ImmutableProcessTime.copyOf(rootProcessTime)
                                                             .withDelayBetweenSteps(rootProcessTime.getDelayBetweenSteps()
                                                                 + overallDelayBetweenSteps);

        ProcessTimeLogger.logOverallProcessTime(LOGGER, overallProcessTime, correlationId);
    }
}
