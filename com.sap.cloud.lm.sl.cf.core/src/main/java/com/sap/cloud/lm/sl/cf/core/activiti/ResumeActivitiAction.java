package com.sap.cloud.lm.sl.cf.core.activiti;

import java.text.MessageFormat;
import java.util.List;

import org.activiti.engine.history.HistoricActivityInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResumeActivitiAction extends ActivitiAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResumeActivitiAction.class);

    public ResumeActivitiAction(ActivitiFacade activitiFacade, String userId) {
        super(activitiFacade, userId);
    }

    @Override
    public void executeAction(String superProcessInstanceId) {
        List<String> activeProcessIds = getActiveExecutionIds(superProcessInstanceId);
        for (String processId : activeProcessIds) {
            executeAppropiateActionOverProcess(processId);
        }
    }

    private void executeAppropiateActionOverProcess(String processId) {
        if (activitiFacade.isProcessInstanceSuspended(processId)) {
            LOGGER.debug("Will try to resume process with id " + processId);
            activitiFacade.activateProcessInstance(processId);
            return;
        }

        if (isProcessInReceiveTask(processId)) {
            activitiFacade.signal(userId, processId);
            return;
        }
        LOGGER.warn(MessageFormat.format("Process with id {0} is in undetermined process state", processId));
    }

    private boolean isProcessInReceiveTask(String processId) {
        List<HistoricActivityInstance> receiveTasksPerProcess = getReceiveTasks(processId);
        return !receiveTasksPerProcess.isEmpty();
    }

    private List<HistoricActivityInstance> getReceiveTasks(String processInstanceId) {
        return activitiFacade.getHistoricActivities("receiveTask", processInstanceId);
    }
}
