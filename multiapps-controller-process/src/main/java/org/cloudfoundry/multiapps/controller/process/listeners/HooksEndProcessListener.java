package org.cloudfoundry.multiapps.controller.process.listeners;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationLogsExporter;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerPersister;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

@Named("hooksEndProcessListener")
public class HooksEndProcessListener extends AbstractProcessExecutionListener {

    protected HooksEndProcessListener(ProgressMessageService progressMessageService, StepLogger.Factory stepLoggerFactory,
                                      ProcessLoggerProvider processLoggerProvider, ProcessLoggerPersister processLoggerPersister,
                                      HistoricOperationEventService historicOperationEventService, FlowableFacade flowableFacade,
                                      ApplicationConfiguration configuration, OperationLogsExporter operationLogsExporter) {
        super(progressMessageService,
              stepLoggerFactory,
              processLoggerProvider,
              processLoggerPersister,
              historicOperationEventService,
              flowableFacade,
              configuration,
              operationLogsExporter);
    }

    @Override
    protected void notifyInternal(DelegateExecution execution) throws Exception {
        setVariableInParentProcess(execution, Variables.MUST_RESET_TIMEOUT.getName(), true);
    }
}
