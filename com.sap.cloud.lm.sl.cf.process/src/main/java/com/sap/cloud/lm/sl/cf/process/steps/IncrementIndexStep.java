package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("incrementIndexStep")
public class IncrementIndexStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncrementIndexStep.class);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        logActivitiTask(context, LOGGER);

        // Continue the iteration over the collection:
        String indexVariableName = (String) context.getVariable(Constants.VAR_INDEX_VARIABLE_NAME);
        ContextUtil.incrementVariable(context, indexVariableName);

        return ExecutionStatus.SUCCESS;
    }

}
