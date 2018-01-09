package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;

import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.core.dao.ContextExtensionDao;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.process.exception.MonitoringException;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

public abstract class AbstractSubProcessMonitorExecution extends AsyncExecution {

    protected ActivitiFacade activitiFacade;

    public AbstractSubProcessMonitorExecution(ActivitiFacade activitiFacade) {
        this.activitiFacade = activitiFacade;
    }

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        String subProcessId = StepsUtil.getSubProcessId(execution.getContext());
        execution.getStepLogger().debug(Messages.STARTING_MONITORING_SUBPROCESS, subProcessId);
        try {
            HistoricProcessInstance subProcess = getSubProcess(execution.getContext(), subProcessId);
            return getSubProcessStatus(subProcess, execution);
        } catch (Exception e) {
            throw new MonitoringException(e, Messages.ERROR_MONITORING_SUBPROCESS, subProcessId);
        }
    }

    private HistoricProcessInstance getSubProcess(DelegateExecution context, String subProcessId) {
        HistoryService historyService = context.getEngineServices().getHistoryService();
        return historyService.createHistoricProcessInstanceQuery().processInstanceId(subProcessId).singleResult();
    }

    private AsyncExecutionState getSubProcessStatus(HistoricProcessInstance subProcess, ExecutionWrapper execution)
        throws MonitoringException {
        ErrorType errorType = getSubProcessErrorType(subProcess, execution.getContextExtensionDao());
        execution.getStepLogger().debug(Messages.ERROR_TYPE_OF_SUBPROCESS, subProcess.getId(), errorType);

        Execution subProcessInstanceExecution = activitiFacade.getProcessExecution(subProcess.getId());
        if (subProcessInstanceExecution != null) {
            String subProcessActivityType = getSubProcessActivityType(subProcess, subProcessInstanceExecution);
            if ("receiveTask".equals(subProcessActivityType)) {
                return suspendProcessInstance(execution);
            }
        }

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
            return AsyncExecutionState.RUNNING;
        }
        return onError(execution.getContext(), errorType);
    }

    private AsyncExecutionState suspendProcessInstance(ExecutionWrapper execution) {
        String processInstanceId = execution.getContext().getProcessInstanceId();
        activitiFacade.suspendProcessInstance(processInstanceId);
        StepsUtil.setStepPhase(execution, StepPhase.POLL);
        return AsyncExecutionState.RUNNING;
    }

    private String getSubProcessActivityType(HistoricProcessInstance subProcess, Execution subProcessInstanceExecution) {
        return activitiFacade.getActivityType(subProcess.getId(), subProcessInstanceExecution.getId(),
            subProcessInstanceExecution.getActivityId());
    }

    private AsyncExecutionState getFinishedProcessStatus(HistoricProcessInstance subProcess, ExecutionWrapper execution,
        ErrorType errorType) throws MonitoringException {
        if (subProcess.getEndTime() == null) {
            return AsyncExecutionState.RUNNING;
        }
        if (subProcess.getDeleteReason() == null) {
            return onSuccess(execution.getContext());
        }
        return onAbort(execution.getContext(), errorType);
    }

    private ErrorType getSubProcessErrorType(HistoricProcessInstance subProcess, ContextExtensionDao contextExtensionDao) {
        return StepsUtil.getErrorType(subProcess.getId(), contextExtensionDao);
    }

    protected abstract AsyncExecutionState onError(DelegateExecution context, ErrorType errorType) throws MonitoringException;

    protected abstract AsyncExecutionState onAbort(DelegateExecution context, ErrorType errorType) throws MonitoringException;

    protected AsyncExecutionState onSuccess(DelegateExecution context) {
        injectProcessVariablesFromSubProcess(context);

        return AsyncExecutionState.FINISHED;
    }

    private void injectProcessVariablesFromSubProcess(DelegateExecution context) {
        String subProcessId = StepsUtil.getSubProcessId(context);
        List<String> processVariablesToInject = getProcessVariablesToInject(context);
        for (String processVariable : processVariablesToInject) {
            HistoricVariableInstance historicVariableInstance = activitiFacade.getHistoricVariableInstance(subProcessId, processVariable);
            if (historicVariableInstance != null) {
                context.setVariable(processVariable, historicVariableInstance.getValue());
            }
        }
    }

    protected abstract List<String> getProcessVariablesToInject(DelegateExecution context);

}
