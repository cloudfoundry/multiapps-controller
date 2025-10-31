package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.util.ClientReleaser;
import org.cloudfoundry.multiapps.controller.process.util.HistoryUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;

public abstract class ProcessAction {

    protected final FlowableFacade flowableFacade;
    protected final List<AdditionalProcessAction> additionalProcessActions;
    protected final OperationService operationService;
    private final CloudControllerClientProvider clientProvider;

    @Inject
    protected ProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions,
                            OperationService operationService, CloudControllerClientProvider clientProvider) {
        this.flowableFacade = flowableFacade;
        this.additionalProcessActions = additionalProcessActions;
        this.operationService = operationService;
        this.clientProvider = clientProvider;
    }

    protected List<String> getActiveExecutionIds(String superProcessInstanceId) {
        List<String> activeHistoricSubProcessIds = flowableFacade.getActiveHistoricSubProcessIds(superProcessInstanceId);
        activeHistoricSubProcessIds.add(0, superProcessInstanceId);
        return activeHistoricSubProcessIds;
    }

    public void execute(UserInfo userInfo, String superProcessInstanceId) {
        for (AdditionalProcessAction additionalProcessAction : filterAdditionalActionsForThisAction()) {
            additionalProcessAction.executeAdditionalProcessAction(superProcessInstanceId);
        }

        executeActualProcessAction(userInfo, superProcessInstanceId);
    }

    private List<AdditionalProcessAction> filterAdditionalActionsForThisAction() {
        return additionalProcessActions.stream()
                                       .filter(additionalAction -> additionalAction.getApplicableAction() == getAction())
                                       .collect(Collectors.toList());
    }

    protected abstract void executeActualProcessAction(UserInfo userInfo, String superProcessInstanceId);

    public abstract Action getAction();

    protected void updateUserIfNecessary(UserInfo userInfo, String executionId, List<String> processIds) {
        HistoryService historyService = flowableFacade.getProcessEngine()
                                                      .getHistoryService();
        String currentUserGuid = HistoryUtil.getVariableValue(historyService, executionId, Variables.USER_GUID.getName());
        if (!userInfo.getId()
                     .equals(currentUserGuid)) {
            ClientReleaser clientReleaser = new ClientReleaser(clientProvider);
            clientReleaser.releaseClientFor(historyService, executionId);
            updateProcessIds(userInfo, processIds);
        }
    }

    private void updateProcessIds(UserInfo userInfo, List<String> processIds) {
        RuntimeService runtimeService = flowableFacade.getProcessEngine()
                                                      .getRuntimeService();
        for (String processId : processIds) {
            runtimeService.setVariable(processId, Variables.USER.getName(), userInfo.getName());
            runtimeService.setVariable(processId, Variables.USER_GUID.getName(), userInfo.getId());
        }
    }

    protected void updateOperationState(String processId, Operation.State newState) {
        Operation operation = operationService.createQuery()
                                              .processId(processId)
                                              .singleResult();
        operation = ImmutableOperation.builder()
                                      .from(operation)
                                      .state(newState)
                                      .build();
        operationService.update(operation, operation);
    }
}