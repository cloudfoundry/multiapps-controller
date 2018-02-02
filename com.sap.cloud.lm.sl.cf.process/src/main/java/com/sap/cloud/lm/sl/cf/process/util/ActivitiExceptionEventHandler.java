package com.sap.cloud.lm.sl.cf.process.util;

import java.sql.Timestamp;
import java.text.MessageFormat;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiExceptionEvent;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;

public class ActivitiExceptionEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiExceptionEventHandler.class);

    private ProgressMessageService progressMessageService;

    public ActivitiExceptionEventHandler(ProgressMessageService progressMessageService) {
        this.progressMessageService = progressMessageService;
    }

    public void handle(ActivitiEvent event) {
        if (!(event instanceof ActivitiExceptionEvent)) {
            return;
        }

        ActivitiExceptionEvent activitiExceptionEvent = (ActivitiExceptionEvent) event;
        String activitiExceptionStackTrace = ExceptionUtils.getStackTrace(activitiExceptionEvent.getCause());
        LOGGER.error(activitiExceptionStackTrace);
        String activitiExceptionMessage = activitiExceptionEvent.getCause().getMessage();

        if (activitiExceptionMessage == null) {
            return;
        }

        try {
            String taskId = getTaskId(event);
            String errorMessage = MessageFormat.format(Messages.EXCEPTION_OCCURED_ERROR_MSG, activitiExceptionMessage);
            progressMessageService.add(new ProgressMessage(event.getProcessInstanceId(), taskId, ProgressMessageType.ERROR, errorMessage,
                new Timestamp(System.currentTimeMillis())));
        } catch (SLException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    private String getTaskId(ActivitiEvent event) {
        VariableInstance variableInstance = event.getEngineServices()
            .getRuntimeService()
            .getVariableInstance(event.getExecutionId(), com.sap.cloud.lm.sl.persistence.message.Constants.INDEXED_STEP_NAME);

        if (variableInstance == null) {
            return getTakIdFromHistoryService(event);
        }

        return variableInstance.getTextValue();
    }

    private String getTakIdFromHistoryService(ActivitiEvent event) {
        HistoricVariableInstance historicVariableInstance = event.getEngineServices()
            .getHistoryService()
            .createHistoricVariableInstanceQuery()
            .executionId(event.getExecutionId())
            .variableName(com.sap.cloud.lm.sl.persistence.message.Constants.INDEXED_STEP_NAME)
            .singleResult();

        if (historicVariableInstance == null) {
            return null;
        }

        return (String) historicVariableInstance.getValue();
    }
}
