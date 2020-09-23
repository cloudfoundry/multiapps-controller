package org.cloudfoundry.multiapps.controller.process.flowable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class FlowableActionFactoryTest {

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

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testAbortAction() {
        Mockito.when(processActionRegistry.getAction(ABORT_ACTION_ID))
               .thenReturn(new AbortProcessAction(facade, List.of(additionalProcessAction), null, null, cloudControllerClientProvider, null, null));
        testAction(ABORT_ACTION_ID, AbortProcessAction.class);
    }

    @Test
    void testRetryAction() {
        Mockito.when(processActionRegistry.getAction(RETRY_ACTION_ID))
               .thenReturn(new RetryProcessAction(facade,
                                                  List.of(additionalProcessAction),
                                                  historicOperationEventService,
                                                  cloudControllerClientProvider));
        testAction(RETRY_ACTION_ID, RetryProcessAction.class);
    }

    @Test
    void testResumeAction() {
        Mockito.when(processActionRegistry.getAction(RESUME_ACTION_ID))
               .thenReturn(new ResumeProcessAction(facade, List.of(additionalProcessAction), cloudControllerClientProvider));
        testAction(RESUME_ACTION_ID, ResumeProcessAction.class);
    }

    private void testAction(Action action, Class<? extends ProcessAction> actionClass) {
        ProcessAction processAction = processActionRegistry.getAction(action);
        assertEquals(processAction.getClass(), actionClass);
    }
}
