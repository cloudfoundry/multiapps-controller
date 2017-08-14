package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;

import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.exception.MonitoringException;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

public abstract class AbstractXS2SubProcessMonitorStep extends AbstractXS2ProcessStepWithBridge {

    static final Logger LOGGER = LoggerFactory.getLogger(AbstractXS2SubProcessMonitorStep.class);

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) {
        String subProcessId = StepsUtil.getSubProcessId(context);
        debug(context, MessageFormat.format(Messages.STARTING_MONITORING_SUBPROCESS, subProcessId), LOGGER);
        try {
            HistoricProcessInstance subProcess = getSubProcess(context, subProcessId);
            return getSubProcessStatus(subProcess, context);
        } catch (Exception e) {
            throw new MonitoringException(e, Messages.ERROR_MONITORING_SUBPROCESS, subProcessId);
        }
    }

    private ExecutionStatus getSubProcessStatus(HistoricProcessInstance subProcess, DelegateExecution context) throws MonitoringException {
        Job executionJob = context.getEngineServices().getManagementService().createJobQuery().processInstanceId(
            subProcess.getId()).singleResult();
        if (executionJob == null) {
            return getFinishedProcessStatus(subProcess, context);
        }

        if (executionJob.getExceptionMessage() == null) {
            return ExecutionStatus.RUNNING;
        }
        return onError(context);
    }

    protected abstract ExecutionStatus onError(DelegateExecution context) throws MonitoringException;

    private ExecutionStatus getFinishedProcessStatus(HistoricProcessInstance subProcess, DelegateExecution context)
        throws MonitoringException {
        if (subProcess.getEndTime() == null) {
            return ExecutionStatus.RUNNING;
        }
        if (subProcess.getDeleteReason() == null) {
            return ExecutionStatus.SUCCESS;
        }
        return onAborted(context);
    }

    protected abstract ExecutionStatus onAborted(DelegateExecution context) throws MonitoringException;

    private HistoricProcessInstance getSubProcess(DelegateExecution context, String subProcessId) {
        HistoryService hisotryService = context.getEngineServices().getHistoryService();
        return hisotryService.createHistoricProcessInstanceQuery().processInstanceId(subProcessId).singleResult();
    }

}
