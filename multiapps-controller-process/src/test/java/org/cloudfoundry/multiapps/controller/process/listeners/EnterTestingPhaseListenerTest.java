package org.cloudfoundry.multiapps.controller.process.listeners;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.OperationQueryImpl;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersister;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.util.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class EnterTestingPhaseListenerTest {

    // needed because of @InjectMocks
    @Mock
    private ProgressMessageService progressMessageService;
    // needed because of @InjectMocks
    @Mock
    private ProcessLoggerProvider processLoggerProvider;
    // needed because of @InjectMocks
    @Mock
    private ProcessLogsPersister processLogsPersister;
    // needed because of @InjectMocks
    @Mock
    private HistoricOperationEventService historicOperationEventService;
    // needed because of @InjectMocks
    @Mock
    private FlowableFacade flowableFacade;
    // needed because of @InjectMocks
    @Mock
    private ApplicationConfiguration configuration;

    @Mock
    private OperationService operationService;
    @Mock
    private OperationQueryImpl operationQuery;
    @Mock
    private StepLogger.Factory stepLoggerFactory;
    @Mock
    private StepLogger stepLogger;

    @InjectMocks
    private EnterTestingPhaseListener listener;

    private DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        prepareOperationService();
        prepareStepLogger();
    }

    @Test
    void testNotifyInternal() {
        listener.notify(execution);
        Operation updatedOperation = ImmutableOperation.builder()
                                                       .state(Operation.State.ACTION_REQUIRED)
                                                       .build();
        Mockito.verify(operationQuery)
               .processId(execution.getProcessInstanceId());
        Mockito.verify(operationService)
               .update(updatedOperation, updatedOperation);
        Mockito.verify(stepLogger)
               .debug(Messages.ENTERING_TESTING_PHASE);
    }

    private void prepareOperationService() {
        operationQuery = Mockito.mock(OperationQueryImpl.class);
        Mockito.when(operationService.createQuery())
               .thenReturn(operationQuery);
        Operation operation = ImmutableOperation.builder()
                                                .state(Operation.State.RUNNING)
                                                .build();
        Mockito.when(operationQuery.processId(Mockito.anyString()))
               .thenReturn(operationQuery);
        Mockito.when(operationQuery.singleResult())
               .thenReturn(operation);
        Mockito.when(operationQuery.processId(Mockito.anyString()))
               .thenReturn(operationQuery);
    }

    private void prepareStepLogger() {
        Mockito.when(stepLoggerFactory.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
               .thenReturn(stepLogger);
    }

}
