package org.cloudfoundry.multiapps.controller.process.listeners;

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

@Named("doNotDeleteServicesListener")
public class DoNotDeleteServicesListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    protected DoNotDeleteServicesListener(ProgressMessageService progressMessageService, StepLogger.Factory stepLoggerFactory,
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
        if (!isRootProcess(execution)) {
            return;
        }
        getStepLogger().warn(Messages.SKIP_SERVICES_DELETION);
    }

}