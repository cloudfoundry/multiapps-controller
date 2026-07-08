package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.Collections;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.mockito.Mockito.times;

class StartProcessActionTest extends ProcessActionTest {

    @Test
    void testResumeExecution() {
        processAction.execute(USER_INFO, PROCESS_GUID);
        Mockito.verify(flowableFacade, times(2))
               .trigger(EXECUTION_ID, Map.of(Variables.USER.getName(), "fake-user", Variables.USER_GUID.getName(), "fake-user-guid"));
        assertStateUpdated(Operation.State.RUNNING);
    }

    @Override
    protected ProcessAction createProcessAction() {
        return new StartProcessAction(flowableFacade, Collections.emptyList(), operationService, cloudControllerClientProvider);
    }

    @Test
    void testStartActionWithUserGuidAndOriginButNotUsername() {
        Logger logger = Mockito.mock(Logger.class);
        ProcessAction startAction = new StartProcessAction(flowableFacade, Collections.emptyList(), operationService,
                                                           cloudControllerClientProvider) {
            @Override
            protected Logger getLogger() {
                return logger;
            }
        };

        startAction.execute(USER_INFO, PROCESS_GUID);
        assertUserInfoLogContent(logger, Action.START);
    }
}
