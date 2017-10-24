package com.sap.cloud.lm.sl.cf.api.activiti;

import java.util.List;

import com.sap.cloud.lm.sl.cf.web.api.model.State;

public class AbortActivitiAction extends ActivitiAction {

    public AbortActivitiAction(ActivitiFacade activitiFacade, String userId) {
        super(activitiFacade, userId);
    }

    @Override
    public void executeAction(String superProcessInstanceId) {
        List<String> executionIds = getActiveExecutionIds(superProcessInstanceId);
        for (String executionId : executionIds) {
            activitiFacade.deleteProcessInstance(userId, executionId, State.ABORTED.value());
        }
    }
}
