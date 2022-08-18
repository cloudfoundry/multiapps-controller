package org.cloudfoundry.multiapps.controller.process.listeners;

import javax.inject.Named;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;

@Named("deployAppSubProcessEndListener")
public class DeployAppSubProcessEndListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

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
