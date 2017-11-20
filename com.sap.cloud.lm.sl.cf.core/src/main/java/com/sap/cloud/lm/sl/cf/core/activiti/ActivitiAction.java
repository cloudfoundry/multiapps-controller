package com.sap.cloud.lm.sl.cf.core.activiti;

import java.util.LinkedList;
import java.util.List;

public abstract class ActivitiAction {

    protected ActivitiFacade activitiFacade;
    protected String userId;

    public ActivitiAction(ActivitiFacade activitiFacade, String userId) {
        this.activitiFacade = activitiFacade;
        this.userId = userId;
    }

    protected List<String> getActiveExecutionIds(String superProcessInstanceId) {
        List<String> activeHistoricSubProcessIds = activitiFacade.getActiveHistoricSubProcessIds(superProcessInstanceId);
        LinkedList<String> subProcessIds = new LinkedList<>(activeHistoricSubProcessIds);
        subProcessIds.addFirst(superProcessInstanceId);
        return subProcessIds;
    }

    public abstract void executeAction(String superProcessInstanceId);
}