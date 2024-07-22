package org.cloudfoundry.multiapps.controller.process.listeners;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersister;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;

@Named("deployAppSubProcessEndListener")
public class DeployAppSubProcessEndListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    @Inject
    protected DeployAppSubProcessEndListener(ProgressMessageService progressMessageService, StepLogger.Factory stepLoggerFactory,
                                             ProcessLoggerProvider processLoggerProvider, ProcessLogsPersister processLogsPersister,
                                             HistoricOperationEventService historicOperationEventService, FlowableFacade flowableFacade,
                                             ApplicationConfiguration configuration) {
        super(progressMessageService,
              stepLoggerFactory,
              processLoggerProvider,
              processLogsPersister,
              historicOperationEventService,
              flowableFacade,
              configuration);
    }

    @Override
    public void notifyInternal(DelegateExecution execution) throws Exception {
        CloudServiceBroker serviceBroker = VariableHandling.get(execution, Variables.CREATED_OR_UPDATED_SERVICE_BROKER);
        if (serviceBroker == null) {
            return;
        }

        CloudApplicationExtended cloudApplication = VariableHandling.get(execution, Variables.APP_TO_PROCESS);
        if (cloudApplication == null) {
            throw new IllegalStateException(Messages.CANNOT_DETERMINE_CURRENT_APPLICATION);
        }

        String moduleName = cloudApplication.getModuleName();
        if (moduleName == null) {
            throw new IllegalStateException(Messages.CANNOT_DETERMINE_MODULE_NAME);
        }

        String exportedVariableName = Constants.VAR_APP_SERVICE_BROKER_VAR_PREFIX + moduleName;
        byte[] binaryJson = JsonUtil.toJsonBinary(serviceBroker);
        setVariableInParentProcess(execution, exportedVariableName, binaryJson);
    }

}