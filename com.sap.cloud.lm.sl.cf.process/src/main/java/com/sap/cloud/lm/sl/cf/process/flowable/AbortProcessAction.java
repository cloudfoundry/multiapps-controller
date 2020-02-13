package com.sap.cloud.lm.sl.cf.process.flowable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.process.util.HistoricOperationEventPersister;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;

@Named
public class AbortProcessAction extends ProcessAction {

    public static final String ACTION_ID_ABORT = "abort";

    private HistoricOperationEventPersister historicEventPersister;
    private OperationService operationService;

    @Inject
    public AbortProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions,
                              HistoricOperationEventPersister historicEventPersister, OperationService operationService) {
        super(flowableFacade, additionalProcessActions);
        this.historicEventPersister = historicEventPersister;
        this.operationService = operationService;
    }

    @Override
    public void executeActualProcessAction(String user, String superProcessInstanceId) {
        flowableFacade.setAbortVariable(superProcessInstanceId);
        releaseOperationLock(superProcessInstanceId);
        historicEventPersister.add(superProcessInstanceId, EventType.ABORTED);
    }

    private void releaseOperationLock(String superProcessInstanceId) {
        getProcessConflictPreventer().releaseLock(superProcessInstanceId);
    }

    protected ProcessConflictPreventer getProcessConflictPreventer() {
        return new ProcessConflictPreventer(operationService);
    }

    @Override
    public String getActionId() {
        return ACTION_ID_ABORT;
    }

}
