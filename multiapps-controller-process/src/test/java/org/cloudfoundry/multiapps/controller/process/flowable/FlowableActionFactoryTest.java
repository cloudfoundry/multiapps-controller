package org.cloudfoundry.multiapps.controller.process.flowable;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FlowableActionFactoryTest {

    private static final Action RESUME_ACTION_ID = Action.RESUME;
    private static final Action RETRY_ACTION_ID = Action.RETRY;
    private static final Action ABORT_ACTION_ID = Action.ABORT;
    @Mock
    FlowableFacade facade;
    @Mock
    ProgressMessageService progressMessageService;
    @Mock
    AdditionalProcessAction additionalProcessAction;
    @Mock
    ProcessActionRegistry processActionRegistry;
    @Mock
    HistoricOperationEventService historicOperationEventService;
    @Mock
    CloudControllerClientProvider cloudControllerClientProvider;

    @Test
    public void testAbortAction() {
        Mockito.when(processActionRegistry.getAction(ABORT_ACTION_ID))
               .thenReturn(new AbortProcessAction(facade,
                                                  Collections.singletonList(additionalProcessAction),
                                                  null,
                                                  null,
                                                  cloudControllerClientProvider));
        testAction(ABORT_ACTION_ID, AbortProcessAction.class);
    }

    @Test
    public void testRetryAction() {
        Mockito.when(processActionRegistry.getAction(RETRY_ACTION_ID))
               .thenReturn(new RetryProcessAction(facade,
                                                  Collections.singletonList(additionalProcessAction),
                                                  historicOperationEventService,
                                                  cloudControllerClientProvider));
        testAction(RETRY_ACTION_ID, RetryProcessAction.class);
    }

    @Test
    public void testResumeAction() {
        Mockito.when(processActionRegistry.getAction(RESUME_ACTION_ID))
               .thenReturn(new ResumeProcessAction(facade,
                                                   Collections.singletonList(additionalProcessAction),
                                                   cloudControllerClientProvider));
        testAction(RESUME_ACTION_ID, ResumeProcessAction.class);
    }

    private void testAction(Action action, Class<? extends ProcessAction> actionClass) {
        ProcessAction processAction = processActionRegistry.getAction(action);
        assertEquals(processAction.getClass(), actionClass);
    }
}
