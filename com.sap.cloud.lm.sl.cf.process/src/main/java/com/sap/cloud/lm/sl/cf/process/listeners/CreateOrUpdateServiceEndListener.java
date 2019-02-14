package com.sap.cloud.lm.sl.cf.process.listeners;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.runtime.Execution;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;

@Component("createOrUpdateServiceEndListener")
public class CreateOrUpdateServiceEndListener implements ExecutionListener {
    
    private static final long serialVersionUID = 1L;
    
    @Override
    public void notify(DelegateExecution context) {
        boolean isServiceUpdated = StepsUtil.getIsServiceUpdated(context);
        String serviceName = StepsUtil.getServiceToProcessName(context);
        if(serviceName == null) {
            throw new IllegalStateException("Not able to determine service update status.");
        }
        String exportedVariableName = Constants.VAR_IS_SERVICE_UPDATED_VAR_PREFIX + serviceName;
        
        RuntimeService runtimeService = Context.getProcessEngineConfiguration().getRuntimeService();
        
        String superExecutionId = context.getParentId();
        Execution superExecutionResult = runtimeService.createExecutionQuery().executionId(superExecutionId).singleResult();
        superExecutionId = superExecutionResult.getSuperExecutionId();
        
        runtimeService.setVariable(superExecutionId, exportedVariableName, isServiceUpdated);
    }
}
