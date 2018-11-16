package com.sap.cloud.lm.sl.cf.core.flowable;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public abstract class ProcessAction {

    protected final FlowableFacade flowableFacade;
    protected final List<AdditionalProcessAction> additionalProcessActions;

    @Inject
    public ProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions) {
        this.flowableFacade = flowableFacade;
        this.additionalProcessActions = additionalProcessActions;
    }

    protected List<String> getActiveExecutionIds(String superProcessInstanceId) {
        List<String> activeHistoricSubProcessIds = flowableFacade.getActiveHistoricSubProcessIds(superProcessInstanceId);
        LinkedList<String> subProcessIds = new LinkedList<>(activeHistoricSubProcessIds);
        subProcessIds.addFirst(superProcessInstanceId);
        return subProcessIds;
    }

    public void execute(String userId, String superProcessInstanceId) {
        for (AdditionalProcessAction additionalProcessAction : filterAdditionalActionsForThisAction()) {
            additionalProcessAction.executeAdditionalProcessAction(superProcessInstanceId);
        }

        executeActualProcessAction(userId, superProcessInstanceId);
    }

    private List<AdditionalProcessAction> filterAdditionalActionsForThisAction() {
        return additionalProcessActions.stream()
            .filter(additionalAction -> additionalAction.getApplicableActionId()
                .equals(getActionId()))
            .collect(Collectors.toList());
    }

    protected abstract void executeActualProcessAction(String userId, String superProcessInstanceId);

    public abstract String getActionId();
}