package com.sap.cloud.lm.sl.cf.core.activiti;

import java.util.LinkedList;
import java.util.List;

public abstract class FlowableAction {

    protected FlowableFacade flowableFacade;
    protected String userId;

    public FlowableAction(FlowableFacade activitiFacade, String userId) {
        this.flowableFacade = activitiFacade;
        this.userId = userId;
    }

    protected List<String> getActiveExecutionIds(String superProcessInstanceId) {
        List<String> activeHistoricSubProcessIds = flowableFacade.getActiveHistoricSubProcessIds(superProcessInstanceId);
        LinkedList<String> subProcessIds = new LinkedList<>(activeHistoricSubProcessIds);
        subProcessIds.addFirst(superProcessInstanceId);
        return subProcessIds;
    }

    public abstract void executeAction(String superProcessInstanceId);
}