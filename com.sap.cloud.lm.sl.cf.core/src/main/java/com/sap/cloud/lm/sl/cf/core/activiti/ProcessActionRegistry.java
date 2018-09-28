package com.sap.cloud.lm.sl.cf.core.activiti;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

@Component
public class ProcessActionRegistry {

    private List<ProcessAction> processActions;

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
