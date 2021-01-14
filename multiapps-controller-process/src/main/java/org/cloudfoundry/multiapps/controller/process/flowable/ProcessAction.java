package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.util.ClientReleaser;
import org.cloudfoundry.multiapps.controller.process.util.HistoryUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.HistoryService;

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

    public void execute(String user, String superProcessInstanceId) {
        for (AdditionalProcessAction additionalProcessAction : filterAdditionalActionsForThisAction()) {
            additionalProcessAction.executeAdditionalProcessAction(superProcessInstanceId);
        }

        executeActualProcessAction(user, superProcessInstanceId);
    }

    private List<AdditionalProcessAction> filterAdditionalActionsForThisAction() {
        return additionalProcessActions.stream()
                                       .filter(additionalAction -> additionalAction.getApplicableAction() == getAction())
                                       .collect(Collectors.toList());
    }

    protected abstract void executeActualProcessAction(String user, String superProcessInstanceId);

    public abstract Action getAction();

    protected void updateUserIfNecessary(String user, String executionId) {
        HistoryService historyService = flowableFacade.getProcessEngine()
                                                      .getHistoryService();
        String currentUser = HistoryUtil.getVariableValue(historyService, executionId, Variables.USER.getName());
        if (!user.equals(currentUser)) {
            ClientReleaser clientReleaser = new ClientReleaser(clientProvider);
            clientReleaser.releaseClientFor(historyService, executionId);
            flowableFacade.getProcessEngine()
                          .getRuntimeService()
                          .setVariable(executionId, Variables.USER.getName(), user);
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