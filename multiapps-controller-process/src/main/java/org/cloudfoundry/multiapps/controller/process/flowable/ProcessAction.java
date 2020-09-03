package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.util.ClientReleaser;
import org.cloudfoundry.multiapps.controller.process.util.HistoryUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.HistoryService;

public abstract class ProcessAction {

    protected final FlowableFacade flowableFacade;
    protected final List<AdditionalProcessAction> additionalProcessActions;
    private final CloudControllerClientProvider clientProvider;

    @Inject
    public ProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions,
                         CloudControllerClientProvider clientProvider) {
        this.flowableFacade = flowableFacade;
        this.additionalProcessActions = additionalProcessActions;
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
                                       .filter(additionalAction -> additionalAction.getApplicableActionId()
                                                                                   .equals(getActionId()))
                                       .collect(Collectors.toList());
    }

    protected abstract void executeActualProcessAction(String user, String superProcessInstanceId);

    public abstract String getActionId();

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
}