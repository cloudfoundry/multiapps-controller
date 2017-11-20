package com.sap.cloud.lm.sl.cf.core.activiti;

import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryActivitiAction extends ActivitiAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryActivitiAction.class);

    public RetryActivitiAction(ActivitiFacade activitiFacade, String userId) {
        super(activitiFacade, userId);
    }

    @Override
    public void executeAction(String superProcessInstanceId) {
        List<String> subProcessIds = getActiveExecutionIds(superProcessInstanceId);
        ListIterator<String> subProcessesIdsIterator = subProcessIds.listIterator(subProcessIds.size());
        while (subProcessesIdsIterator.hasPrevious()) {
            String subProcessId = subProcessesIdsIterator.previous();
            retryProcess(subProcessId);
        }
    }

    private void retryProcess(String subProcessId) {
        try {
            activitiFacade.executeJob(userId, subProcessId);
        } catch (RuntimeException e) {
            // Consider the retry as successful. The execution error could be later obtained through
            // the getError() method.
            LOGGER.error(Messages.ACTIVITI_JOB_RETRY_FAILED, e);
        }
    }
}
