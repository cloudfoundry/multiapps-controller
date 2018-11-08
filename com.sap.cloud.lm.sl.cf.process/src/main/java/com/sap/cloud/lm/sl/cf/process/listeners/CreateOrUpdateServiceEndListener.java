package com.sap.cloud.lm.sl.cf.process.listeners;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;

@Component("createOrUpdateServiceEndListener")
public class CreateOrUpdateServiceEndListener extends AbstractProcessExecutionListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateOrUpdateServiceEndListener.class);

    @Override
    protected void notifyInternal(DelegateExecution context) throws Exception {
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

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
