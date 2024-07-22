package org.cloudfoundry.multiapps.controller.process.listeners;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersister;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

@Named("manageAppServiceBindingEndListener")
public class ManageAppServiceBindingEndListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    private final ProcessTypeParser processTypeParser;

    @Inject
    protected ManageAppServiceBindingEndListener(ProgressMessageService progressMessageService, StepLogger.Factory stepLoggerFactory,
                                                 ProcessLoggerProvider processLoggerProvider, ProcessLogsPersister processLogsPersister,
                                                 HistoricOperationEventService historicOperationEventService, FlowableFacade flowableFacade,
                                                 ApplicationConfiguration configuration, ProcessTypeParser processTypeParser) {
        super(progressMessageService,
              stepLoggerFactory,
              processLoggerProvider,
              processLogsPersister,
              historicOperationEventService,
              flowableFacade,
              configuration);
        this.processTypeParser = processTypeParser;
    }

    @Override
    public void notifyInternal(DelegateExecution execution) {
        ProcessType processType = processTypeParser.getProcessType(execution);
        if (processType == ProcessType.UNDEPLOY) {
            return;
        }
        CloudApplicationExtended app = VariableHandling.get(execution, Variables.APP_TO_PROCESS);
        String service = VariableHandling.get(execution, Variables.SERVICE_TO_UNBIND_BIND);
        if (app == null || service == null) {
            return;
        }
        boolean shouldUnbindService = VariableHandling.get(execution, Variables.SHOULD_UNBIND_SERVICE_FROM_APP);
        boolean shouldBindService = VariableHandling.get(execution, Variables.SHOULD_BIND_SERVICE_TO_APP);

        String exportedVariableName = buildExportedVariableName(app.getName(), service);

        setVariableInParentProcess(execution, exportedVariableName, shouldUnbindService || shouldBindService);
    }

    public static String buildExportedVariableName(String appName, String service) {
        StringBuilder variableNameBuilder = new StringBuilder();
        variableNameBuilder.append(Constants.VAR_IS_APPLICATION_SERVICE_BINDING_UPDATED_VAR_PREFIX);
        variableNameBuilder.append(appName);
        variableNameBuilder.append('_');
        variableNameBuilder.append(service);
        return variableNameBuilder.toString();
    }
}