package com.sap.cloud.lm.sl.cf.process.util;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableExceptionEvent;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.dao.ProgressMessageDao;
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;

public class FlowableExceptionEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableExceptionEventHandler.class);

    private ProgressMessageDao progressMessageDao;
    private FlowableFacade flowableFacade;

    public FlowableExceptionEventHandler(ProgressMessageDao progressMessageDao, FlowableFacade flowableFacade) {
        this.progressMessageDao = progressMessageDao;
        this.flowableFacade = flowableFacade;
    }

    public void handle(FlowableEvent event) {
        if (!(event instanceof FlowableExceptionEvent) && !(event instanceof FlowableEngineEvent)) {
            return;
        }

        FlowableExceptionEvent flowableExceptionEvent = getFlowableExceptionEvent(event);
        String flowableExceptionStackTrace = ExceptionUtils.getStackTrace(flowableExceptionEvent.getCause());
        LOGGER.error(flowableExceptionStackTrace);
        String flowableExceptionMessage = flowableExceptionEvent.getCause()
            .getMessage();

        if (flowableExceptionMessage == null) {
            return;
        }

        try {
            tryToPreserveFlowableException(event, flowableExceptionMessage);
        } catch (SLException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    protected FlowableExceptionEvent getFlowableExceptionEvent(FlowableEvent event) {
        return (FlowableExceptionEvent) event;
    }

    private void tryToPreserveFlowableException(FlowableEvent event, String flowableExceptionMessage) {
        FlowableEngineEvent flowableEngineEvent = (FlowableEngineEvent) event;

        String processInstanceId = flowableFacade.getProcessInstanceId(flowableEngineEvent.getExecutionId());
        if (isErrorProgressMessagePresented(processInstanceId)) {
            return;
        }

        String taskId = getCurrentTaskId(flowableEngineEvent);
        String errorMessage = MessageFormat.format(Messages.UNEXPECTED_ERROR, flowableExceptionMessage);
        progressMessageDao.add(ImmutableProgressMessage.builder()
            .processId(processInstanceId)
            .taskId(taskId)
            .type(ProgressMessageType.ERROR)
            .text(errorMessage)
            .timestamp(getCurrentTimestamp())
            .build());
    }

    private boolean isErrorProgressMessagePresented(String processInstanceId) {
        List<ProgressMessage> progressMessages = progressMessageDao.find(processInstanceId);
        return progressMessages.stream()
            .anyMatch(this::isErrorMessage);
    }

    private boolean isErrorMessage(ProgressMessage message) {
        return message.getType() == ProgressMessageType.ERROR;
    }

    private String getCurrentTaskId(FlowableEngineEvent flowableEngineEvent) {
        Execution currentExecutionForProces = findCurrentExecution(flowableEngineEvent);

        return currentExecutionForProces != null ? currentExecutionForProces.getActivityId()
            : flowableFacade.getCurrentTaskId(flowableEngineEvent.getExecutionId());
    }

    private Execution findCurrentExecution(FlowableEngineEvent flowableEngineEvent) {
        try {
            // This is needed because when there are parallel CallActivity, the query will return multiple results for just one Execution
            List<Execution> currentExecutionsForProcess = getProcessEngineConfiguration().getRuntimeService()
                .createExecutionQuery()
                .executionId(flowableEngineEvent.getExecutionId())
                .processInstanceId(flowableEngineEvent.getProcessInstanceId())
                .list();

            // Based on the above comment, one of the executions will have null activityId(because it will be the monitoring one) and thus
            // should be excluded from the list of executions
            return CommonUtil.isNullOrEmpty(currentExecutionsForProcess) ? null
                : findCurrentExecution(currentExecutionsForProcess);
        } catch (Throwable e) {
            return null;
        }
    }

    protected ProcessEngineConfiguration getProcessEngineConfiguration() {
        return Context.getProcessEngineConfiguration();
    }

    private Execution findCurrentExecution(List<Execution> currentExecutionsForProcess) {
        return currentExecutionsForProcess.stream()
            .filter(execution -> execution.getActivityId() != null)
            .findFirst()
            .orElse(null);
    }

    protected Date getCurrentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }
}
