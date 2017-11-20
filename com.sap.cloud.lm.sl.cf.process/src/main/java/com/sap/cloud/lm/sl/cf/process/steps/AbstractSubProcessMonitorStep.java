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
import com.sap.cloud.lm.sl.cf.core.dao.ContextExtensionDao;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.process.exception.MonitoringException;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

public abstract class AbstractSubProcessMonitorStep extends AsyncStepOperation {

    @Inject
    private ActivitiFacade activitiFacade;

    @Override
    public ExecutionStatus executeOperation(ExecutionWrapper execution) {
        String subProcessId = StepsUtil.getSubProcessId(execution.getContext());
        execution.getStepLogger().debug(Messages.STARTING_MONITORING_SUBPROCESS, subProcessId);
        try {
            HistoricProcessInstance subProcess = getSubProcess(execution.getContext(), subProcessId);
            return getSubProcessStatus(subProcess, execution);
        } catch (Exception e) {
            StepsUtil.setStepPhase(execution, StepPhase.POLL);
            throw new MonitoringException(e, Messages.ERROR_MONITORING_SUBPROCESS, subProcessId);
        }
    }

    private HistoricProcessInstance getSubProcess(DelegateExecution context, String subProcessId) {
        HistoryService historyService = context.getEngineServices().getHistoryService();
        return historyService.createHistoricProcessInstanceQuery().processInstanceId(subProcessId).singleResult();
    }

    private ExecutionStatus getSubProcessStatus(HistoricProcessInstance subProcess, ExecutionWrapper execution) throws MonitoringException {
        ErrorType errorType = getSubProcessErrorType(subProcess, execution.getContextExtensionDao());
        execution.getStepLogger().debug(Messages.ERROR_TYPE_OF_SUBPROCESS, subProcess.getId(), errorType);
        Job executionJob = execution.getContext()
            .getEngineServices()
            .getManagementService()
            .createJobQuery()
            .processInstanceId(subProcess.getId())
            .singleResult();
        if (executionJob == null) {
            return getFinishedProcessStatus(subProcess, execution, errorType);
        }

        if (executionJob.getExceptionMessage() == null) {
            StepsUtil.setStepPhase(execution, StepPhase.POLL);
            return ExecutionStatus.RUNNING;
        }
        StepsUtil.setStepPhase(execution, StepPhase.POLL);
        return onError(execution.getContext(), errorType);
    }

    private ExecutionStatus getFinishedProcessStatus(HistoricProcessInstance subProcess, ExecutionWrapper execution, ErrorType errorType)
        throws MonitoringException {
        if (subProcess.getEndTime() == null) {
            StepsUtil.setStepPhase(execution, StepPhase.POLL);
            return ExecutionStatus.RUNNING;
        }
        if (subProcess.getDeleteReason() == null) {
            StepsUtil.setStepPhase(execution, StepPhase.EXECUTE);
            return onSuccess(execution.getContext());
        }
        StepsUtil.setStepPhase(execution, StepPhase.EXECUTE);
        return onAbort(execution.getContext(), errorType);
    }

    private ErrorType getSubProcessErrorType(HistoricProcessInstance subProcess, ContextExtensionDao contextExtensionDao) {
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
