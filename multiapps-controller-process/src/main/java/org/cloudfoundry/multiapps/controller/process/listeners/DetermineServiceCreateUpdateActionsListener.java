package org.cloudfoundry.multiapps.controller.process.listeners;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.util.ServiceAction;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

@Named("determineServiceCreateUpdateActionsListener")
public class DetermineServiceCreateUpdateActionsListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    protected void notifyInternal(DelegateExecution execution) throws Exception {
        List<ServiceAction> serviceActions = VariableHandling.get(execution, Variables.SERVICE_ACTIONS_TO_EXCECUTE);
        CloudServiceInstanceExtended serviceToProcess = VariableHandling.get(execution, Variables.SERVICE_TO_PROCESS);

        String exportedVariableName = buildExportedVariableName(serviceToProcess.getName());

        setVariableInParentProcess(execution, exportedVariableName, serviceActions);
    }

    public static String buildExportedVariableName(String serviceName) {
        StringBuilder variableNameBuilder = new StringBuilder();
        variableNameBuilder.append(Constants.VAR_SERVICE_ACTIONS_TO_EXECUTE);
        variableNameBuilder.append(serviceName);
        return variableNameBuilder.toString();
    }

}
