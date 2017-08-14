package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("monitorMtaDeploySubProcessStep")
public class MonitorMtaDeploySubProcessStep extends AbstractXS2SubProcessMonitorStep {

    @Override
    public String getLogicalStepName() {
        return StartMtaDeploySubProcessStep.class.getSimpleName();
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_MTARS_INDEX;
    }

    @Override
    protected ExecutionStatus onError(DelegateExecution context) {
        return ExecutionStatus.FAILED;
    }

    @Override
    protected ExecutionStatus onAborted(DelegateExecution context) {
        return ExecutionStatus.FAILED;
    }

}
