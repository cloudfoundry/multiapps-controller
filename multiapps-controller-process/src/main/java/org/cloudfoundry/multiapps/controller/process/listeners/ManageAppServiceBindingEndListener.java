package org.cloudfoundry.multiapps.controller.process.listeners;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

@Named("manageAppServiceBindingEndListener")
public class ManageAppServiceBindingEndListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    @Inject
    private ProcessTypeParser processTypeParser;

    @Override
    public void notifyInternal(DelegateExecution execution) {
        ProcessType processType = processTypeParser.getProcessType(execution);
        if (processType == ProcessType.UNDEPLOY) {
            return;
        }
        CloudApplicationExtended app = VariableHandling.get(execution, Variables.APP_TO_PROCESS);
        String service = VariableHandling.get(execution, Variables.SERVICE_TO_UNBIND_BIND);
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
