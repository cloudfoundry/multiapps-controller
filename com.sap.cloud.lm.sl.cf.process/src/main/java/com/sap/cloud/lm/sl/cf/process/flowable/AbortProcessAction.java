package com.sap.cloud.lm.sl.cf.process.flowable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.process.util.HistoricOperationEventPersister;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

@Named
public class AbortProcessAction extends ProcessAction {

    public static final String ACTION_ID_ABORT = "abort";
    private final HistoricOperationEventPersister historicOperationEventPersister;

    @Inject
    public AbortProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions,
                              HistoricOperationEventPersister historicOperationEventPersister) {
        super(flowableFacade, additionalProcessActions);
        this.historicOperationEventPersister = historicOperationEventPersister;
    }

    @Override
    public void executeActualProcessAction(String user, String superProcessInstanceId) {
        historicOperationEventPersister.add(superProcessInstanceId, HistoricOperationEvent.EventType.ABORTED);
        if (flowableFacade.isProcessInstanceSuspended(superProcessInstanceId)) {
            flowableFacade.activateProcessInstance(superProcessInstanceId);
        }
        // TODO: The Delete Reason won't be indispensable anymore and should be deleted after this version is stable.
        flowableFacade.deleteProcessInstance(superProcessInstanceId, Operation.State.ABORTED.name());
    }

    @Override
    public String getActionId() {
        return ACTION_ID_ABORT;
    }

}
