package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.flowable.engine.HistoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.runtime.Execution;
import org.flowable.job.api.Job;
import org.flowable.variable.api.history.HistoricVariableInstance;

import com.sap.cloud.lm.sl.cf.core.activiti.FlowableFacade;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.process.exception.MonitoringException;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

public abstract class AbstractSubProcessMonitorExecution implements AsyncExecution {

    protected FlowableFacade activitiFacade;

    public AbstractSubProcessMonitorExecution(FlowableFacade activitiFacade) {
        this.activitiFacade = activitiFacade;
    }

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        String subProcessId = StepsUtil.getSubProcessId(execution.getContext());
        execution.getStepLogger()
            .debug(Messages.STARTING_MONITORING_SUBPROCESS, subProcessId);
        try {
            HistoricProcessInstance subProcess = getSubProcess(execution.getContext(), subProcessId);
            return getSubProcessStatus(execution, subProcess);
        } catch (Exception e) {
            throw new MonitoringException(e, Messages.ERROR_MONITORING_SUBPROCESS, subProcessId);
        }
    }

    private HistoricProcessInstance getSubProcess(DelegateExecution context, String subProcessId) {
        HistoryService historyService = Context.getProcessEngineConfiguration()
            .getHistoryService();
        return historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(subProcessId)
            .singleResult();
    }

    private AsyncExecutionState getSubProcessStatus(ExecutionWrapper execution, HistoricProcessInstance subProcess) {
        ErrorType errorType = StepsUtil.getErrorType(execution.getContext());

        List<Execution> subProcessInstanceExecutionsAtReceiveTask = activitiFacade.findExecutionsAtReceiveTask(subProcess.getId());
        if (subProcessInstanceExecutionsAtReceiveTask != null && !subProcessInstanceExecutionsAtReceiveTask.isEmpty()) {
            return suspendProcessInstance(execution);
        }

        List<Job> deadLetterJobs = Context.getProcessEngineConfiguration()
            .getManagementService()
            .createDeadLetterJobQuery()
            .processInstanceId(subProcess.getId())
            .list();
        if (!deadLetterJobs.isEmpty()) {
            return onError(execution.getContext(), errorType);
        }

        Job executionJob = Context.getProcessEngineConfiguration()
            .getManagementService()
            .createJobQuery()
            .processInstanceId(subProcess.getId())
            .singleResult();
        if (executionJob == null) {
            return getFinishedProcessStatus(subProcess, execution, errorType);
        }

        execution.getStepLogger()
            .debug(Messages.ERROR_TYPE_OF_SUBPROCESS, subProcess.getId(), errorType);
        return AsyncExecutionState.RUNNING;
    }

    private AsyncExecutionState suspendProcessInstance(ExecutionWrapper execution) {
        String processInstanceId = execution.getContext()
            .getProcessInstanceId();
        activitiFacade.suspendProcessInstance(processInstanceId);
        StepsUtil.setStepPhase(execution.getContext(), StepPhase.POLL);
        return AsyncExecutionState.RUNNING;
    }

    private AsyncExecutionState getFinishedProcessStatus(HistoricProcessInstance subProcess, ExecutionWrapper execution,
        ErrorType errorType) {
        if (subProcess.getEndTime() == null) {
            return AsyncExecutionState.RUNNING;
        }
        if (subProcess.getDeleteReason() == null) {
            return onSuccess(execution.getContext());
        }
        return onAbort(execution.getContext(), errorType);
    }

    protected abstract AsyncExecutionState onError(DelegateExecution context, ErrorType errorType);

    protected abstract AsyncExecutionState onAbort(DelegateExecution context, ErrorType errorType);

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
