package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("incrementIndexStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class IncrementIndexStep extends AbstractXS2ProcessStep {

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        getStepLogger().logActivitiTask();

        // Continue the iteration over the collection:
        String indexVariableName = (String) context.getVariable(Constants.VAR_INDEX_VARIABLE_NAME);
        ContextUtil.incrementVariable(context, indexVariableName);

        return ExecutionStatus.SUCCESS;
    }

}
