package com.sap.cloud.lm.sl.cf.process.util;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.cf.services.TypedServiceOperationState;
import com.sap.cloud.lm.sl.cf.process.steps.ExecutionWrapper;

@Component
public class ServiceProgressReporter {

    public void reportOverallProgress(ExecutionWrapper execution, List<ServiceOperation> lastServicesOperationS,
                                      Map<String, ServiceOperationType> triggeredServiceOperations) {
        List<TypedServiceOperationState> nonFinalStates = getNonFinalStates(lastServicesOperationS);
        String nonFinalStateStrings = getStateStrings(nonFinalStates);

        int doneOperations = triggeredServiceOperations.size() - nonFinalStates.size();
        if (!nonFinalStateStrings.isEmpty()) {
            execution.getStepLogger()
                     .info("{0} of {1} done, ({2})", doneOperations, triggeredServiceOperations.size(), nonFinalStateStrings);
        } else {
            execution.getStepLogger()
                     .info("{0} of {0} done", triggeredServiceOperations.size());
        }
    }

    private List<TypedServiceOperationState> getNonFinalStates(List<ServiceOperation> operations) {
        return operations.stream()
                         .map(TypedServiceOperationState::fromServiceOperation)
                         .filter(this::isServiceOperationStateNotDone)
                         .collect(Collectors.toList());
    }

    private boolean isServiceOperationStateNotDone(TypedServiceOperationState state) {
        return state != TypedServiceOperationState.DONE;
    }

    private String getStateStrings(List<TypedServiceOperationState> states) {
        Map<TypedServiceOperationState, Long> stateCounts = getStateCounts(states);
        return stateCounts.entrySet()
                          .stream()
                          .map(this::formatStateCount)
                          .collect(Collectors.joining(","));

    }

    private String formatStateCount(Entry<TypedServiceOperationState, Long> stateCount) {
        return format("{0} {1}", stateCount.getValue(), stateCount.getKey()
                                                                  .toString()
                                                                  .toLowerCase());
    }

    private Map<TypedServiceOperationState, Long> getStateCounts(List<TypedServiceOperationState> serviceOperationStates) {
        return serviceOperationStates.stream()
                                     .collect(Collectors.groupingBy(state -> state, TreeMap::new, Collectors.counting()));
    }
}
