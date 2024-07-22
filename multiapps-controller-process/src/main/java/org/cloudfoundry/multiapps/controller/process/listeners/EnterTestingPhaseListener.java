package org.cloudfoundry.multiapps.controller.process.listeners;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersister;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

@Named("enterTestingPhaseListener")
public class EnterTestingPhaseListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    private final OperationService operationService;

    @Inject
    protected EnterTestingPhaseListener(ProgressMessageService progressMessageService, StepLogger.Factory stepLoggerFactory,
                                        ProcessLoggerProvider processLoggerProvider, ProcessLogsPersister processLogsPersister,
                                        HistoricOperationEventService historicOperationEventService, FlowableFacade flowableFacade,
                                        ApplicationConfiguration configuration, OperationService operationService) {
        super(progressMessageService,
              stepLoggerFactory,
              processLoggerProvider,
              processLogsPersister,
              historicOperationEventService,
              flowableFacade,
              configuration);
        this.operationService = operationService;
    }

    @Override
    protected void notifyInternal(DelegateExecution execution) {
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
        setStateToActionRequired(correlationId);
        getStepLogger().debug(Messages.ENTERING_TESTING_PHASE);
    }

    private void setStateToActionRequired(String processId) {
        Operation operation = operationService.createQuery()
                                              .processId(processId)
                                              .singleResult();
        operation = ImmutableOperation.builder()
                                      .from(operation)
                                      .state(Operation.State.ACTION_REQUIRED)
                                      .build();
        operationService.update(operation, operation);
    }

}