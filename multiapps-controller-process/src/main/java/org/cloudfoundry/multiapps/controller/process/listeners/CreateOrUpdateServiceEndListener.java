package org.cloudfoundry.multiapps.controller.process.listeners;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.runtime.Execution;

@Named("createOrUpdateServiceEndListener")
public class CreateOrUpdateServiceEndListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    public void notify(DelegateExecution execution) {
        RuntimeService runtimeService = Context.getProcessEngineConfiguration()
                                               .getRuntimeService();

        String superExecutionId = execution.getParentId();
        Execution superExecutionResult = runtimeService.createExecutionQuery()
                                                       .executionId(superExecutionId)
                                                       .singleResult();
        superExecutionId = superExecutionResult.getSuperExecutionId();

        boolean isServiceUpdated = VariableHandling.get(execution, Variables.IS_SERVICE_UPDATED);
        String serviceName = VariableHandling.get(execution, Variables.SERVICE_TO_PROCESS_NAME);
        if (serviceName == null) {
            throw new IllegalStateException("Unable to determine service update status.");
        }

        setServiceUpdatedInParentProcess(runtimeService, superExecutionId, isServiceUpdated, serviceName);
        setDynamicResolvableParameterInParentProcess(execution, runtimeService, superExecutionId, serviceName);
    }

    private void setServiceUpdatedInParentProcess(RuntimeService runtimeService, String superExecutionId, boolean isServiceUpdated,
                                                  String serviceName) {
        String exportedVariableName = Constants.VAR_IS_SERVICE_UPDATED_VAR_PREFIX + serviceName;

        runtimeService.setVariable(superExecutionId, exportedVariableName, isServiceUpdated);
    }

    private void setDynamicResolvableParameterInParentProcess(DelegateExecution execution, RuntimeService runtimeService,
                                                              String superExecutionId, String serviceName) {
        DynamicResolvableParameter dynamicResolvableParameter = VariableHandling.get(execution, Variables.DYNAMIC_RESOLVABLE_PARAMETER);
        if (dynamicResolvableParameter != null) {
            String exportServiceGuidVariableName = Constants.VAR_SERVICE_INSTANCE_GUID_PREFIX + serviceName;
            runtimeService.setVariable(superExecutionId, exportServiceGuidVariableName,
                                       Variables.DYNAMIC_RESOLVABLE_PARAMETER.getSerializer()
                                                                             .serialize(dynamicResolvableParameter));
        }
    }
}
