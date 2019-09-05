package com.sap.cloud.lm.sl.cf.process.flowable;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.sap.cloud.lm.sl.cf.core.persistence.service.ProgressMessageService;

@RunWith(MockitoJUnitRunner.class)
public class FlowableActionFactoryTest {

    private static final String RESUME_ACTION_ID = "resume";
    private static final String RETRY_ACTION_ID = "retry";
    private static final String ABORT_ACTION_ID = "abort";
    @Mock
    FlowableFacade facade;
    @Mock
    ProgressMessageService progressMessageService;
    @Mock
    AdditionalProcessAction additionalProcessAction;
    @Mock
    ProcessActionRegistry processActionRegistry;

    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAbortAction() {
        Mockito.when(processActionRegistry.getAction(ABORT_ACTION_ID))
               .thenReturn(new AbortProcessAction(facade, Arrays.asList(additionalProcessAction)));
        testAction(ABORT_ACTION_ID, AbortProcessAction.class);
    }

    @Test
    public void testRetryAction() {
        Mockito.when(processActionRegistry.getAction(RETRY_ACTION_ID))
               .thenReturn(new RetryProcessAction(facade, Arrays.asList(additionalProcessAction)));
        testAction(RETRY_ACTION_ID, RetryProcessAction.class);
    }

    @Test
    public void testResumeAction() {
        Mockito.when(processActionRegistry.getAction(RESUME_ACTION_ID))
               .thenReturn(new ResumeProcessAction(facade, Arrays.asList(additionalProcessAction)));
        testAction(RESUME_ACTION_ID, ResumeProcessAction.class);
    }

    private void testAction(String actionId, Class<? extends ProcessAction> actionClass) {
        ProcessAction action = processActionRegistry.getAction(actionId);
        assertEquals(action.getClass(), actionClass);
    }
}
