package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.OperationQueryImpl;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

abstract class ProcessActionTest {

    private static final Supplier<String> RANDOM_UUID_SUPPLIER = () -> UUID.randomUUID()
                                                                           .toString();
    static final String EXECUTION_ID = RANDOM_UUID_SUPPLIER.get();
    static final String PROCESS_GUID = RANDOM_UUID_SUPPLIER.get();
    static final String SUBPROCESS_1_ID = RANDOM_UUID_SUPPLIER.get();
    static final String SUBPROCESS_2_ID = RANDOM_UUID_SUPPLIER.get();
    static final OAuth2AccessTokenWithAdditionalInfo token = mockToken();
    static final UserInfo USER_INFO = new UserInfo("fake-user-guid", "fake-user", token);

    protected ProcessAction processAction;
    @Mock
    protected FlowableFacade flowableFacade;
    @Mock
    protected CloudControllerClientProvider cloudControllerClientProvider;
    @Mock
    protected OperationService operationService;
    @Mock
    private ProcessEngine processEngine;
    @Mock
    private HistoryService historyService;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private OperationQueryImpl operationQuery;

    @BeforeEach
    void initMocks() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        prepareFlowableFacade();
        prepareProcessEngine();
        prepareHistoryService();
        prepareOperationService();
        processAction = createProcessAction();
    }

    private void prepareFlowableFacade() {
        List<String> subprocessesIds = getSubprocessesIds();
        Mockito.when(flowableFacade.getActiveHistoricSubProcessIds(PROCESS_GUID))
               .thenReturn(subprocessesIds);
        List<Execution> mockedExecutions = getMockedExecutions();
        Mockito.when(flowableFacade.findExecutionsAtReceiveTask(PROCESS_GUID))
               .thenReturn(mockedExecutions);
        Mockito.when(flowableFacade.findExecutionsAtReceiveTask(SUBPROCESS_1_ID))
               .thenReturn(mockedExecutions);
        Mockito.when(flowableFacade.getProcessEngine())
               .thenReturn(processEngine);
    }

    private void prepareProcessEngine() {
        Mockito.when(processEngine.getHistoryService())
               .thenReturn(historyService);
        Mockito.when(processEngine.getRuntimeService())
               .thenReturn(runtimeService);
    }

    protected List<Execution> getMockedExecutions() {
        Execution execution = Mockito.mock(Execution.class);
        Mockito.when(execution.getId())
               .thenReturn(EXECUTION_ID);
        return List.of(execution);
    }

    private List<String> getSubprocessesIds() {
        List<String> subprocesses = new ArrayList<>();
        subprocesses.add(SUBPROCESS_1_ID);
        subprocesses.add(SUBPROCESS_2_ID);
        return subprocesses;
    }

    private void prepareHistoryService() {
        HistoricVariableInstanceQuery historicVariableInstanceQuery = Mockito.mock(HistoricVariableInstanceQuery.class);
        Mockito.when(historicVariableInstanceQuery.processInstanceId(anyString()))
               .thenReturn(historicVariableInstanceQuery);
        Mockito.when(historicVariableInstanceQuery.variableName(anyString()))
               .thenReturn(historicVariableInstanceQuery);
        Mockito.when(historyService.createHistoricVariableInstanceQuery())
               .thenReturn(historicVariableInstanceQuery);
    }

    private void prepareOperationService() {
        Mockito.when(operationService.createQuery())
               .thenReturn(operationQuery);
        Operation operation = ImmutableOperation.builder()
                                                .state(Operation.State.RUNNING)
                                                .build();
        Mockito.when(operationQuery.singleResult())
               .thenReturn(operation);
        Mockito.when(operationQuery.processId(Mockito.anyString()))
               .thenReturn(operationQuery);
    }

    protected void assertStateUpdated(Operation.State state) {
        Operation operation = ImmutableOperation.builder()
                                                .state(state)
                                                .build();
        Mockito.verify(operationService)
               .update(operation, operation);
    }

    protected void assertUserInfoLogContent(Logger logger, Action expectedAction) {
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger, Mockito.atLeastOnce())
               .info(argumentCaptor.capture());
        String logMessageToVerify = argumentCaptor.getAllValues()
                                                  .stream()
                                                  .filter(message -> message != null && message.contains(
                                                      "MTA deployment process action executed"))
                                                  .findFirst()
                                                  .orElseThrow(
                                                      () -> new AssertionError("No log line was printed. Captured logs: "
                                                                                   + argumentCaptor.getAllValues()));
        assertTrue(logMessageToVerify.contains("processId: \"" + PROCESS_GUID + "\""),
                   "Must contain the processId. Actual: " + logMessageToVerify);
        assertTrue(logMessageToVerify.contains("action: \"" + expectedAction + "\""),
                   "Must contain the action. Actual: " + logMessageToVerify);
        assertTrue(logMessageToVerify.contains("userGUID: \"fake-user-guid\""),
                   "Must contain the userGUID. Actual: " + logMessageToVerify);
        assertTrue(logMessageToVerify.contains("origin: \"test-origin\""),
                   "Must contain the origin. Actual: " + logMessageToVerify);
        assertFalse(logMessageToVerify.contains("fake-user\""),
                    "Must NOT contain the username. Actual: " + logMessageToVerify);
    }

    protected abstract ProcessAction createProcessAction();

    private static OAuth2AccessTokenWithAdditionalInfo mockToken() {
        OAuth2AccessTokenWithAdditionalInfo token = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        Mockito.when(token.getAdditionalInfo())
               .thenReturn(Map.of("origin", "test-origin"));
        return token;
    }
}
