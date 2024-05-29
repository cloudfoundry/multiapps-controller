package org.cloudfoundry.multiapps.controller.process.listeners;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerPersister;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.flowable.engine.delegate.DelegateExecution;

@Named("leaveTestingPhaseListener")
public class LeaveTestingPhaseListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    @Inject
    protected LeaveTestingPhaseListener(ProgressMessageService progressMessageService, StepLogger.Factory stepLoggerFactory,
                                        ProcessLoggerProvider processLoggerProvider, ProcessLoggerPersister processLoggerPersister,
                                        HistoricOperationEventService historicOperationEventService, FlowableFacade flowableFacade,
                                        ApplicationConfiguration configuration) {
        super(progressMessageService,
              stepLoggerFactory,
              processLoggerProvider,
              processLoggerPersister,
              historicOperationEventService,
              flowableFacade,
              configuration);
    }

    @Override
    protected void notifyInternal(DelegateExecution execution) {
        getStepLogger().debug(Messages.LEAVING_TESTING_PHASE);
    }

}