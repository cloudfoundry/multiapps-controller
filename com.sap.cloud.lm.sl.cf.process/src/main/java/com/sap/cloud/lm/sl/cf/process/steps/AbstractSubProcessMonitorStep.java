package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;

import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.runtime.Job;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.process.exception.MonitoringException;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

public abstract class AbstractSubProcessMonitorStep extends AbstractProcessStepWithBridge {

    @Inject
    private ActivitiFacade activitiFacade;

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        String subProcessId = StepsUtil.getSubProcessId(context);
        getStepLogger().debug(Messages.STARTING_MONITORING_SUBPROCESS, subProcessId);
        try {
            HistoricProcessInstance subProcess = getSubProcess(context, subProcessId);
            return getSubProcessStatus(subProcess, context);
        } catch (Exception e) {
            throw new MonitoringException(e, Messages.ERROR_MONITORING_SUBPROCESS, subProcessId);
        }
    }

    private HistoricProcessInstance getSubProcess(DelegateExecution context, String subProcessId) {
        HistoryService historyService = context.getEngineServices().getHistoryService();
        return historyService.createHistoricProcessInstanceQuery().processInstanceId(subProcessId).singleResult();
    }

    private ExecutionStatus getSubProcessStatus(HistoricProcessInstance subProcess, DelegateExecution context) throws MonitoringException {
        ErrorType errorType = getSubProcessErrorType(subProcess);
        getStepLogger().debug(Messages.ERROR_TYPE_OF_SUBPROCESS, subProcess.getId(), errorType);
        Job executionJob = context.getEngineServices()
            .getManagementService()
            .createJobQuery()
            .processInstanceId(subProcess.getId())
            .singleResult();
        if (executionJob == null) {
            return getFinishedProcessStatus(subProcess, context, errorType);
        }

        if (executionJob.getExceptionMessage() == null) {
            return ExecutionStatus.RUNNING;
        }
        return onError(context, errorType);
    }

    private ExecutionStatus getFinishedProcessStatus(HistoricProcessInstance subProcess, DelegateExecution context, ErrorType errorType)
        throws MonitoringException {
        if (subProcess.getEndTime() == null) {
            return ExecutionStatus.RUNNING;
        }
        if (subProcess.getDeleteReason() == null) {
            return onSuccess(context);
        }
        return onAbort(context, errorType);
    }

    private ErrorType getSubProcessErrorType(HistoricProcessInstance subProcess) {
        return StepsUtil.getErrorType(subProcess.getId(), contextExtensionDao);
    }

    protected abstract ExecutionStatus onError(DelegateExecution context, ErrorType errorType) throws MonitoringException;

    protected abstract ExecutionStatus onAbort(DelegateExecution context, ErrorType errorType) throws MonitoringException;

    protected ExecutionStatus onSuccess(DelegateExecution context) {
        injectProcessVariablesFromSubProcess(context);
        return ExecutionStatus.SUCCESS;
    }

    private void injectProcessVariablesFromSubProcess(DelegateExecution context) {
        String subProcessId = StepsUtil.getSubProcessId(context);
        List<String> processVariablesToInject = getProcessVariablesToInject();
        for (String processVariable : processVariablesToInject) {
            HistoricVariableInstance historicVariableInstance = activitiFacade.getHistoricVariableInstance(subProcessId, processVariable);
            if (historicVariableInstance != null) {
                context.setVariable(processVariable, historicVariableInstance.getValue());
            }
        }
    }

    protected abstract List<String> getProcessVariablesToInject();

}
