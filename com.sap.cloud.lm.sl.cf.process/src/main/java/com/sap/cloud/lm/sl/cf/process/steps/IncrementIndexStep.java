package com.sap.cloud.lm.sl.cf.process.steps;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("incrementIndexStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class IncrementIndexStep extends SyncActivitiStep {

    @Override
    protected ExecutionStatus executeStep(ExecutionWrapper execution) {
        getStepLogger().logActivitiTask();

        // Continue the iteration over the collection:
        String indexVariableName = (String) execution.getContext().getVariable(Constants.VAR_INDEX_VARIABLE_NAME);
        ContextUtil.incrementVariable(execution.getContext(), indexVariableName);

        return ExecutionStatus.SUCCESS;
    }

}
