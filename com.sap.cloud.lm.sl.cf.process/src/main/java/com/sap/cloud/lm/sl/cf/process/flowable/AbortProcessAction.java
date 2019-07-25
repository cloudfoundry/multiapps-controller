package com.sap.cloud.lm.sl.cf.process.flowable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.web.api.model.State;

@Named
public class AbortProcessAction extends ProcessAction {

    public static final String ACTION_ID_ABORT = "abort";

    @Inject
    public AbortProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions) {
        super(flowableFacade, additionalProcessActions);
    }

    @Override
    public void executeActualProcessAction(String userId, String superProcessInstanceId) {
        if (flowableFacade.isProcessInstanceSuspended(superProcessInstanceId)) {
            flowableFacade.activateProcessInstance(superProcessInstanceId);
        }
        flowableFacade.deleteProcessInstance(userId, superProcessInstanceId, State.ABORTED.name());
    }

    @Override
    public String getActionId() {
        return ACTION_ID_ABORT;
    }

}
