package com.sap.cloud.lm.sl.cf.core.activiti;

import java.util.Locale;

public class FlowableActionFactory {

    public static final String ACTION_ID_RETRY = "retry";
    public static final String ACTION_ID_ABORT = "abort";
    public static final String ACTION_ID_RESUME = "resume";

    public static FlowableAction getAction(String actionId, FlowableFacade flowableFacade, String userId) {
        switch (actionId.toLowerCase(Locale.ROOT)) {
            case ACTION_ID_ABORT:
                return new AbortFlowableAction(flowableFacade, userId);
            case ACTION_ID_RETRY:
                return new RetryFlowableAction(flowableFacade, userId);
            case ACTION_ID_RESUME:
                return new ResumeFlowableAction(flowableFacade, userId);
            default:
                return null;
        }
    }

}
