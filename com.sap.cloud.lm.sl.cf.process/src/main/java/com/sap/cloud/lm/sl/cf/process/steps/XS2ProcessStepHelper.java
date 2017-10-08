package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.dao.ContextExtensionDao;
import com.sap.cloud.lm.sl.cf.core.model.ContextExtension;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.slp.services.ProcessLoggerProviderFactory;
import com.sap.cloud.lm.sl.slp.steps.SLProcessStepHelper;
import com.sap.cloud.lm.sl.slp.steps.StepIndexProvider;
import com.sap.cloud.lm.sl.slp.util.AbstractProcessComponentUtil;

public class XS2ProcessStepHelper extends SLProcessStepHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(XS2ProcessStepHelper.class);

    private ContextExtensionDao contextExtensionDao;

    public XS2ProcessStepHelper(ProgressMessageService progressMessageService, ProcessLoggerProviderFactory processLoggerProviderFactory,
        StepIndexProvider stepIndexProvider, ContextExtensionDao contextExtensionDao) {
        super(progressMessageService, processLoggerProviderFactory, stepIndexProvider);
        this.contextExtensionDao = contextExtensionDao;
    }

    @Override
    protected void postExecuteStep(DelegateExecution context, ExecutionStatus status) {
        logDebug(context, MessageFormat.format(com.sap.cloud.lm.sl.slp.message.Messages.STEP_FINISHED, context.getCurrentActivityName()));

        AbstractProcessComponentUtil.appendLogs(context, processLoggerProviderFactory);
    }

    @Override
    protected void deletePreviousExecutionData(DelegateExecution context) {
        super.deletePreviousExecutionData(context);
        String processId = context.getProcessInstanceId();
        ContextExtension contextExtension = contextExtensionDao.find(processId, Constants.VAR_ERROR_TYPE);
        if (contextExtension == null) {
            return;
        }
        LOGGER.debug(MessageFormat.format(Messages.DELETING_CONTEXT_EXTENSION_WITH_ID_NAME_AND_VALUE_FOR_PROCESS, contextExtension.getId(),
            contextExtension.getName(), contextExtension.getValue(), processId));
        contextExtensionDao.remove(contextExtension.getId());
    }

    @Override
    protected void logException(DelegateExecution context, Throwable t) {
        super.logException(context, t);
        if (t instanceof ContentException) {
            StepsUtil.setErrorType(context.getProcessInstanceId(), contextExtensionDao, ErrorType.CONTENT_ERROR);
        } else {
            StepsUtil.setErrorType(context.getProcessInstanceId(), contextExtensionDao, ErrorType.UNKNOWN_ERROR);
        }
    }

}
