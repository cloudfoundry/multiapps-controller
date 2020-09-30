package org.cloudfoundry.multiapps.controller.process.flowable;

import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

abstract class ProcessActionTest {

    private static final Supplier<String> RANDOM_UUID_SUPPLIER = () -> UUID.randomUUID()
                                                                           .toString();
    static final String EXECUTION_ID = RANDOM_UUID_SUPPLIER.get();
    static final String PROCESS_GUID = RANDOM_UUID_SUPPLIER.get();
    static final String SUBPROCESS_1_ID = RANDOM_UUID_SUPPLIER.get();
    static final String SUBPROCESS_2_ID = RANDOM_UUID_SUPPLIER.get();

    protected ProcessAction processAction;
    @Mock
    protected FlowableFacade flowableFacade;
    @Mock
    protected CloudControllerClientProvider cloudControllerClientProvider;
    @Mock
    private ProcessEngine processEngine;
    @Mock
    private HistoryService historyService;
    @Mock
    private RuntimeService runtimeService;

    @BeforeEach
    void initMocks() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        prepareFlowableFacade();
        prepareProcessEngine();
        prepareHistoryService();
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

    protected abstract ProcessAction createProcessAction();
}
