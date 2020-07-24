package org.cloudfoundry.multiapps.controller.process.listeners;

import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.runtime.Execution;

@Named("deployAppSubProcessEndListener")
public class DeployAppSubProcessEndListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    public void notify(DelegateExecution execution) {
        CloudServiceBroker cloudServiceBrokerExtended = VariableHandling.get(execution, Variables.CREATED_OR_UPDATED_SERVICE_BROKER);

        if (cloudServiceBrokerExtended != null) {
            setVariableInParentProcess(execution, Constants.VAR_APP_SERVICE_BROKER_VAR_PREFIX, cloudServiceBrokerExtended);
        }
    }

    private void setVariableInParentProcess(DelegateExecution execution, String variablePrefix, Object variableValue) {
        CloudApplicationExtended cloudApplication = VariableHandling.get(execution, Variables.APP_TO_PROCESS);
        if (cloudApplication == null) {
            throw new IllegalStateException(Messages.CANNOT_DETERMINE_CURRENT_APPLICATION);
        }

        String moduleName = cloudApplication.getModuleName();
        if (moduleName == null) {
            throw new IllegalStateException(Messages.CANNOT_DETERMINE_MODULE_NAME);
        }
        String exportedVariableName = variablePrefix + moduleName;

        RuntimeService runtimeService = Context.getProcessEngineConfiguration()
                                               .getRuntimeService();

        String superExecutionId = execution.getParentId();
        Execution superExecutionResult = runtimeService.createExecutionQuery()
                                                       .executionId(superExecutionId)
                                                       .singleResult();
        superExecutionId = superExecutionResult.getSuperExecutionId();

        byte[] binaryJson = variableValue == null ? null : JsonUtil.toJsonBinary(variableValue);
        runtimeService.setVariable(superExecutionId, exportedVariableName, binaryJson);
    }

}
