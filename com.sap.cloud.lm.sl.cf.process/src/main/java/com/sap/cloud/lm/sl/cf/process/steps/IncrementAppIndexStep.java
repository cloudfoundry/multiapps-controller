package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("incrementAppIndexStep")
public class IncrementAppIndexStep extends AbstractXS2ProcessStep {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(IncrementAppIndexStep.class);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        logActivitiTask(context, LOGGER);

        // Continue the iteration over the applications list:
        ContextUtil.incrementVariable(context, Constants.VAR_APPS_INDEX);

        return ExecutionStatus.SUCCESS;
    }

}
