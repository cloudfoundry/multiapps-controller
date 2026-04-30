package org.cloudfoundry.multiapps.controller.process.listeners;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationLogsExporter;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerPersister;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

@Named("exportCloudLoggingConfigurationListener")
public class ExportCloudLoggingConfigurationListener extends AbstractProcessExecutionListener {

    @Inject
    protected ExportCloudLoggingConfigurationListener(ProgressMessageService progressMessageService, StepLogger.Factory stepLoggerFactory,
                                                      ProcessLoggerProvider processLoggerProvider,
                                                      ProcessLoggerPersister processLoggerPersister,
                                                      HistoricOperationEventService historicOperationEventService,
                                                      FlowableFacade flowableFacade, ApplicationConfiguration configuration,
                                                      OperationLogsExporter operationLogsExporter) {
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
        LoggingConfiguration loggingConfiguration = VariableHandling.get(execution, Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION);
        if (loggingConfiguration == null) {
            return;
        }
        String parentProcessInstanceId = VariableHandling.get(execution, Variables.PARENT_PROCESS_INSTANCE_ID);
        if (parentProcessInstanceId != null && !parentProcessInstanceId.isEmpty()) {
            setVariableInParentProcessXSA(execution, Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION.getName(),
                                          Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION.getSerializer()
                                                                                          .serialize(loggingConfiguration));
        } else if (hasSuperExecution(execution)) {
            setVariableInParentProcess(execution, Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION.getName(),
                                       Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION.getSerializer()
                                                                                       .serialize(loggingConfiguration));
        } else {
            VariableHandling.set(execution, Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION, loggingConfiguration);
        }
    }
}
