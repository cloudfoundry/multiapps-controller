package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.activiti.FlowableFacade;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.exception.MonitoringException;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

public class MonitorAppDeploySubProcessExecution extends AbstractSubProcessMonitorExecution {

    public MonitorAppDeploySubProcessExecution(FlowableFacade activitiFacade) {
        super(activitiFacade);
    }

    @Override
    protected AsyncExecutionState onError(DelegateExecution context, ErrorType errorType) {
        String subProcessId = StepsUtil.getSubProcessId(context);
        throw new MonitoringException(Messages.SUB_PROCESS_HAS_FAILED, subProcessId);
    }

    @Override
    protected AsyncExecutionState onAbort(DelegateExecution context, ErrorType errorType) {
        String subProcessId = StepsUtil.getSubProcessId(context);
        throw new MonitoringException(Messages.SUB_PROCESS_HAS_BEEN_ABORTED, subProcessId);
    }

    @Override
    protected List<String> getProcessVariablesToInject(DelegateExecution context) {
        return Arrays.asList(Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT);
    }

}
