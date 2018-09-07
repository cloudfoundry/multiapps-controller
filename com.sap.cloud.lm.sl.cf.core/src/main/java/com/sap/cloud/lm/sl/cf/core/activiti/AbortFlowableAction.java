package com.sap.cloud.lm.sl.cf.core.activiti;

import com.sap.cloud.lm.sl.cf.web.api.model.State;

public class AbortFlowableAction extends FlowableAction {

    public AbortFlowableAction(FlowableFacade flowableFacade, String userId) {
        super(flowableFacade, userId);
    }

    @Override
    public void executeAction(String superProcessInstanceId) {
        if (flowableFacade.isProcessInstanceSuspended(superProcessInstanceId)) {
            flowableFacade.activateProcessInstance(superProcessInstanceId);
        }
        flowableFacade.deleteProcessInstance(userId, superProcessInstanceId, State.ABORTED.value());
    }

}
