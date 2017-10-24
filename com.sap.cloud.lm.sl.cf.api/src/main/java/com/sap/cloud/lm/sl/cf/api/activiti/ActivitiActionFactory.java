package com.sap.cloud.lm.sl.cf.api.activiti;

public class ActivitiActionFactory {

    public static final String ACTION_ID_RETRY = "retry";
    public static final String ACTION_ID_ABORT = "abort";
    public static final String ACTION_ID_RESUME = "resume";

    public static ActivitiAction getAction(String actionId, ActivitiFacade activitiFacade, String userId) {
        switch (actionId.toLowerCase()) {
            case ACTION_ID_ABORT:
                return new AbortActivitiAction(activitiFacade, userId);
            case ACTION_ID_RETRY:
                return new RetryActivitiAction(activitiFacade, userId);
            case ACTION_ID_RESUME:
                return new ResumeActivitiAction(activitiFacade, userId);
            default:
                return null;
        }
    }

}
