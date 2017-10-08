package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.exception.MonitoringException;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("monitorAppDeploySubProcessStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MonitorAppDeploySubProcessStep extends AbstractXS2SubProcessMonitorStep {

    @Override
    public String getLogicalStepName() {
        return StartAppDeploySubProcessStep.class.getSimpleName();
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_APPS_INDEX;
    }

    @Override
    protected ExecutionStatus onError(DelegateExecution context, ErrorType errorType) throws MonitoringException {
        String subProcessId = StepsUtil.getSubProcessId(context);
        throw new MonitoringException(Messages.SUB_PROCESS_HAS_FAILED, subProcessId);
    }

    @Override
    protected ExecutionStatus onAbort(DelegateExecution context, ErrorType errorType) throws MonitoringException {
        String subProcessId = StepsUtil.getSubProcessId(context);
        throw new MonitoringException(Messages.SUB_PROCESS_HAS_BEEN_ABORTED, subProcessId);
    }

}
