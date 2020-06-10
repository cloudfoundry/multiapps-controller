package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTimeCalculator.ProcessTime;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTimeCalculator.ProcessTimeLogger;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.controller.processes.metering.MicrometerNotifier;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class OperationTimeAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationTimeAggregator.class);

    private ProcessTimeCalculator processTimeCalculator;
    private FlowableFacade flowableFacade;
    private MicrometerNotifier micrometerNotifier;

    @Inject
    public OperationTimeAggregator(FlowableFacade flowableFacade, MicrometerNotifier micrometerNotifier) {
        this.flowableFacade = flowableFacade;
        this.processTimeCalculator = new ProcessTimeCalculator(flowableFacade);
        this.micrometerNotifier = micrometerNotifier;
    }

    public void aggregateOperationTime(DelegateExecution execution, Operation.State state) {
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
        List<String> historicSubProcesses = flowableFacade.getHistoricSubProcessIds(correlationId);
        historicSubProcesses.add(correlationId);

        Map<String, ProcessTime> processTimesForSubProcesses = historicSubProcesses.stream()
                                                                                   .collect(Collectors.toMap(processId -> processId,
                                                                                                             processTimeCalculator::calculate));
        processTimesForSubProcesses.forEach((key, value) -> logProcessTimeIndividually(value, correlationId,
                key));

        ProcessTime rootProcessTime = processTimesForSubProcesses.get(correlationId);
        micrometerNotifier.recordOverallTime(execution, state, rootProcessTime.getProcessDuration());
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
