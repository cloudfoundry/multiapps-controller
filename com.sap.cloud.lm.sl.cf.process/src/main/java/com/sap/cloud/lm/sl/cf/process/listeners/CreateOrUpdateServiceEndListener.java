package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Named;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.runtime.Execution;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.variables.VariableHandling;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("createOrUpdateServiceEndListener")
public class CreateOrUpdateServiceEndListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    public void notify(DelegateExecution execution) {
        boolean isServiceUpdated = VariableHandling.get(execution, Variables.IS_SERVICE_UPDATED);
        String serviceName = VariableHandling.get(execution, Variables.SERVICE_TO_PROCESS_NAME);
        if (serviceName == null) {
            throw new IllegalStateException("Not able to determine service update status.");
        }
        String exportedVariableName = Constants.VAR_IS_SERVICE_UPDATED_VAR_PREFIX + serviceName;

        RuntimeService runtimeService = Context.getProcessEngineConfiguration()
                                               .getRuntimeService();

        String superExecutionId = execution.getParentId();
        Execution superExecutionResult = runtimeService.createExecutionQuery()
                                                       .executionId(superExecutionId)
                                                       .singleResult();
        superExecutionId = superExecutionResult.getSuperExecutionId();

        runtimeService.setVariable(superExecutionId, exportedVariableName, isServiceUpdated);

    }
}
