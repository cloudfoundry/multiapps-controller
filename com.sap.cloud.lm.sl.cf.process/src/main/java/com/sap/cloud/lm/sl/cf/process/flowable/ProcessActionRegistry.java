package com.sap.cloud.lm.sl.cf.process.flowable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ProcessActionRegistry {

    private final List<ProcessAction> processActions;

    @Inject
    public ProcessActionRegistry(List<ProcessAction> processActions) {
        this.processActions = processActions;
    }

    public ProcessAction getAction(String actionId) {
        return processActions.stream()
                             .filter(action -> actionId.equals(action.getActionId()))
                             .findFirst()
                             .orElse(null);
    }

}
