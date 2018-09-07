package com.sap.cloud.lm.sl.cf.core.activiti;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mockito.Mock;

public class ActivitiActionFactoryTest {

    private static final String RESUME_ACTION_ID = "resume";
    private static final String RETRY_ACTION_ID = "retry";
    private static final String USER_ID = "test-user";
    private static final String ABORT_ACTION_ID = "abort";
    @Mock
    FlowableFacade facade;

    @Test
    public void testAbortAction() {
        testAction(ABORT_ACTION_ID, AbortFlowableAction.class);
    }

    @Test
    public void testRetryAction() {
        testAction(RETRY_ACTION_ID, RetryFlowableAction.class);
    }

    @Test
    public void testResumeAction() {
        testAction(RESUME_ACTION_ID, ResumeFlowableAction.class);
    }

    private void testAction(String actionId, Class<? extends FlowableAction> actionClass) {
        FlowableAction action = FlowableActionFactory.getAction(actionId, facade, USER_ID);
        assertEquals(action.getClass(), actionClass);
        action = FlowableActionFactory.getAction(actionId.toUpperCase(), facade, USER_ID);
        assertEquals(action.getClass(), actionClass);
    }
}
