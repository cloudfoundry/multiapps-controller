package com.sap.cloud.lm.sl.cf.core.activiti;

import java.text.MessageFormat;
import java.util.List;

import org.flowable.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResumeFlowableAction extends FlowableAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResumeFlowableAction.class);

    public ResumeFlowableAction(FlowableFacade flowableFacade, String userId) {
        super(flowableFacade, userId);
    }

    @Override
    public void executeAction(String superProcessInstanceId) {
        List<String> activeProcessIds = getActiveExecutionIds(superProcessInstanceId);
        for (String processId : activeProcessIds) {
            executeAppropiateActionOverProcess(processId);
        }
    }

    private void executeAppropiateActionOverProcess(String processId) {
        List<Execution> executionsAtReceiveTask = flowableFacade.findExecutionsAtReceiveTask(processId);
        if (!executionsAtReceiveTask.isEmpty()) {
            executionsAtReceiveTask.stream()
                .forEach(execution -> flowableFacade.trigger(userId, execution.getId()));
            return;
        }
        LOGGER.warn(MessageFormat.format("Process with id {0} is in undetermined process state", processId));
    }

}
