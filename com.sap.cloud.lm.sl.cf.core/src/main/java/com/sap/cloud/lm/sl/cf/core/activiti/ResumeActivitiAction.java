package com.sap.cloud.lm.sl.cf.core.activiti;

import java.util.List;

import org.activiti.engine.runtime.Execution;
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
        String processInReceiveTask = findProcessInReceiveTask(activeProcessIds);
        if (processInReceiveTask == null) {
            LOGGER.warn("There is no process at a receiveTask activity");
            return;
        }
        activitiFacade.signal(userId, processInReceiveTask);
    }

    private String findProcessInReceiveTask(List<String> activeProcessIds) {
        for (String processId : activeProcessIds) {
            Execution processExecution = activitiFacade.getProcessExecution(processId);
            String activitiType = activitiFacade.getActivityType(processId, processExecution.getActivityId());
            if (activitiType.equals("receiveTask")) {
                return processId;
            }
        }
        return null;
    }
}
