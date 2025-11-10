package org.cloudfoundry.multiapps.controller.process.flowable;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ResumeProcessAction extends ProcessAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResumeProcessAction.class);

    @Inject
    public ResumeProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions,
                               OperationService operationService, CloudControllerClientProvider cloudControllerClientProvider) {
        super(flowableFacade, additionalProcessActions, operationService, cloudControllerClientProvider);
    }

    @Override
    public void executeActualProcessAction(UserInfo userInfo, String superProcessInstanceId) {
        List<String> activeProcessIds = getActiveExecutionIds(superProcessInstanceId);
        List<String> processesAtReceiveTask = activeProcessIds.stream()
                                                              .filter(processId -> !flowableFacade.findExecutionsAtReceiveTask(processId)
                                                                                                  .isEmpty())
                                                              .collect(Collectors.toList());
        updateUserIfNecessary(userInfo, superProcessInstanceId, activeProcessIds);
        for (String processAtReceiveTask : processesAtReceiveTask) {
            triggerProcessInstance(userInfo, processAtReceiveTask);
        }
        updateOperationState(superProcessInstanceId, Operation.State.RUNNING);
    }

    private void triggerProcessInstance(UserInfo userInfo, String processId) {
        List<Execution> executionsAtReceiveTask = flowableFacade.findExecutionsAtReceiveTask(processId);
        if (executionsAtReceiveTask.isEmpty()) {
            LOGGER.warn(MessageFormat.format("Process with id {0} is in undetermined process state", processId));
            return;
        }
        for (Execution execution : executionsAtReceiveTask) {
            flowableFacade.trigger(execution.getId(),
                                   Map.of(Variables.USER.getName(), userInfo.getName(), Variables.USER_GUID.getName(), userInfo.getId()));
        }
    }

    @Override
    public Action getAction() {
        return Action.RESUME;
    }

}
