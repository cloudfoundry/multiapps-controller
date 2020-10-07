package org.cloudfoundry.multiapps.controller.process.util;

import static java.text.MessageFormat.format;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.core.model.TypedServiceOperationState;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;

@Named
public class ServiceProgressReporter {

    public void reportOverallProgress(ProcessContext context, Collection<ServiceOperation> lastServicesOperations,
                                      Map<String, ServiceOperation.Type> triggeredServiceOperations) {
        List<TypedServiceOperationState> nonFinalStates = getNonFinalStates(lastServicesOperations);
        String nonFinalStateStrings = getStateStrings(nonFinalStates);

        int doneOperations = triggeredServiceOperations.size() - nonFinalStates.size();
        if (!nonFinalStateStrings.isEmpty()) {
            context.getStepLogger()
                   .info("{0} of {1} done, ({2})", doneOperations, triggeredServiceOperations.size(), nonFinalStateStrings);
        } else {
            context.getStepLogger()
                   .info("{0} of {0} done", triggeredServiceOperations.size());
        }
    }

    private List<TypedServiceOperationState> getNonFinalStates(Collection<ServiceOperation> operations) {
        return operations.stream()
                         .map(TypedServiceOperationState::fromServiceOperation)
                         .filter(state -> state != TypedServiceOperationState.DONE)
                         .collect(Collectors.toList());
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
