package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableHistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.persistence.query.HistoricOperationEventQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.HistoricOperationEventService;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

public class AbortedOperationsCleanerTest {

    @Mock
    private HistoricOperationEventService historicOperationEventService;
    @Mock
    private FlowableFacade flowableFacade;
    @InjectMocks
    private AbortedOperationsCleaner abortedOperationsCleaner;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExecuteWithNoAbortedOperations() {
        prepareMocksWithProcesses(Collections.emptyList());

        abortedOperationsCleaner.execute(new Date()); // Passed argument is not used.

        Mockito.verify(historicOperationEventService)
               .createQuery();
        Mockito.verifyNoInteractions(flowableFacade);
    }

    @Test
    public void testExecuteWithOperationsInFinalState() {
        prepareMocksWithProcesses(Arrays.asList(new CustomProcess("foo", false), new CustomProcess("bar", false)));

        abortedOperationsCleaner.execute(new Date()); // Passed argument is not used.

        Mockito.verify(flowableFacade, Mockito.times(2))
               .getProcessInstance(anyString());
        Mockito.verify(flowableFacade, never())
               .deleteProcessInstance(anyString(), anyString());
    }

    @Test
    public void testExecute() {
        prepareMocksWithProcesses(Arrays.asList(new CustomProcess("foo", true), new CustomProcess("bar", true)));

        abortedOperationsCleaner.execute(new Date()); // Passed argument is not used.

        Mockito.verify(flowableFacade, Mockito.times(2))
               .getProcessInstance(anyString());
        Mockito.verify(flowableFacade)
               .deleteProcessInstance("foo", Operation.State.ABORTED.name());
        Mockito.verify(flowableFacade)
               .deleteProcessInstance("bar", Operation.State.ABORTED.name());
    }

    @Test
    public void testExecuteWithMixedOperations() {
        prepareMocksWithProcesses(Arrays.asList(new CustomProcess("foo", true), new CustomProcess("bar", false)));

        abortedOperationsCleaner.execute(new Date()); // Passed argument is not used.

        Mockito.verify(flowableFacade, Mockito.times(2))
               .getProcessInstance(anyString());
        Mockito.verify(flowableFacade)
               .deleteProcessInstance("foo", Operation.State.ABORTED.name());
        Mockito.verify(flowableFacade, Mockito.never())
               .deleteProcessInstance("bar", Operation.State.ABORTED.name());
    }

    private void prepareMocksWithProcesses(List<CustomProcess> customProcesses) {
        HistoricOperationEventQuery historicOperationEventQuery = Mockito.mock(HistoricOperationEventQuery.class, Mockito.RETURNS_SELF);
        Mockito.when(historicOperationEventQuery.list())
               .thenReturn(createAbortedEvents(customProcesses));
        Mockito.when(historicOperationEventService.createQuery())
               .thenReturn(historicOperationEventQuery);
        customProcesses.stream()
                       .filter(customProcess -> customProcess.isActive)
                       .forEach(customProcess -> Mockito.when(flowableFacade.getProcessInstance(customProcess.processId))
                                                        .thenReturn(Mockito.mock(ProcessInstance.class)));

    }

    private List<HistoricOperationEvent> createAbortedEvents(List<CustomProcess> customProcesses) {
        return customProcesses.stream()
                              .map(this::createAbortedEvent)
                              .collect(Collectors.toList());
    }

    private HistoricOperationEvent createAbortedEvent(CustomProcess customProcess) {
        return ImmutableHistoricOperationEvent.builder()
                                              .processId(customProcess.processId)
                                              .type(EventType.ABORTED)
                                              .build();
    }

    private class CustomProcess {
        String processId;
        boolean isActive;

        public CustomProcess(String processId, boolean isActive) {
            this.processId = processId;
            this.isActive = isActive;
        }

    }

}
