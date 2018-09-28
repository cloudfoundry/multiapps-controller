package com.sap.cloud.lm.sl.cf.core.activiti;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.web.api.model.State;

@Named
public class AbortProcessAction extends ProcessAction {

    public static final String ACTION_ID_ABORT = "abort";

    @Inject
    public AbortProcessAction(FlowableFacade activitiFacade, List<AdditionalProcessAction> additionalProcessActions) {
        super(activitiFacade, additionalProcessActions);
    }

    @Override
    public void executeActualProcessAction(String userId, String superProcessInstanceId) {
        if (flowableFacade.isProcessInstanceSuspended(superProcessInstanceId)) {
            flowableFacade.activateProcessInstance(superProcessInstanceId);
        }
        flowableFacade.deleteProcessInstance(userId, superProcessInstanceId, State.ABORTED.value());
    }

    @Override
    public String getActionId() {
        return ACTION_ID_ABORT;
    }

}
