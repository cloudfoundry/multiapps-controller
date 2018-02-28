package com.sap.cloud.lm.sl.cf.process.util;

import java.sql.Timestamp;
import java.text.MessageFormat;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.persistence.services.ProcessLoggerProviderFactory;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;

/**
 * The purpose of this class is to group logging of progress messages and process logs in a single place.
 *
 */
public class StepLogger implements UserMessageLogger {

    public static final String PARENT_LOGGER = "com.sap.cloud.lm.sl.xs2";

    protected DelegateExecution context;
    protected ProgressMessageService progressMessageService;
    protected ProcessLoggerProviderFactory processLoggerProviderFactory;
    protected Logger simpleStepLogger;

    public StepLogger(DelegateExecution context, ProgressMessageService progressMessageService,
        ProcessLoggerProviderFactory processLoggerProviderFactory, Logger simpleStepLogger) {
        this.context = context;
        this.progressMessageService = progressMessageService;
        this.processLoggerProviderFactory = processLoggerProviderFactory;
        this.simpleStepLogger = simpleStepLogger;
    }

    public void logActivitiTask() {
        String message = MessageFormat.format(Messages.EXECUTING_TASK, context.getId(), context.getCurrentActivityId());
        debug(message);
    }

    public void infoWithoutProgressMessage(String pattern, Object... arguments) {
        infoWithoutProgressMessage(MessageFormat.format(pattern, arguments));
    }

    public void infoWithoutProgressMessage(String message) {
        simpleStepLogger.info(message);
        getProcessLogger().info(getPrefix(simpleStepLogger) + message);
    }

    public void info(String pattern, Object... arguments) {
        info(MessageFormat.format(pattern, arguments));
    }

    public void info(String message) {
        infoWithoutProgressMessage(message);
        sendProgressMessage(message, ProgressMessageType.INFO);
    }

    public void errorWithoutProgressMessage(Exception e, String pattern, Object... arguments) {
        errorWithoutProgressMessage(e, MessageFormat.format(pattern, arguments));
    }

    public void errorWithoutProgressMessage(Exception e, String message) {
        errorWithoutProgressMessage(getExtendedMessage(message, e));
    }

    public void errorWithoutProgressMessage(String pattern, Object... arguments) {
        errorWithoutProgressMessage(MessageFormat.format(pattern, arguments));
    }

    public void errorWithoutProgressMessage(String message) {
        simpleStepLogger.error(message);
        getProcessLogger().error(getPrefix(simpleStepLogger) + message);
    }

    public void error(Exception e, String pattern, Object... arguments) {
        error(e, MessageFormat.format(pattern, arguments));
    }

    public void error(Exception e, String message) {
        error(getExtendedMessage(message, e));
    }

    public void error(String pattern, Object... arguments) {
        error(MessageFormat.format(pattern, arguments));
    }

    public void error(String message) {
        errorWithoutProgressMessage(message);
        sendProgressMessage(message, ProgressMessageType.ERROR);
    }

    public void warnWithoutProgressMessage(Exception e, String pattern, Object... arguments) {
        warnWithoutProgressMessage(e, MessageFormat.format(pattern, arguments));
    }

    public void warnWithoutProgressMessage(Exception e, String message) {
        simpleStepLogger.warn(message, e);
        getProcessLogger().warn(getPrefix(simpleStepLogger) + message, e);
    }

    public void warnWithoutProgressMessage(String pattern, Object... arguments) {
        warnWithoutProgressMessage(MessageFormat.format(pattern, arguments));
    }

    public void warnWithoutProgressMessage(String message) {
        simpleStepLogger.warn(message);
        getProcessLogger().warn(getPrefix(simpleStepLogger) + message);
    }

    public void warn(Exception e, String pattern, Object... arguments) {
        warn(e, MessageFormat.format(pattern, arguments));
    }

    public void warn(Exception e, String message) {
        warnWithoutProgressMessage(e, message);
        sendProgressMessage(getExtendedMessage(message, e), ProgressMessageType.WARNING);
    }

    public void warn(String pattern, Object... arguments) {
        warn(MessageFormat.format(pattern, arguments));
    }

    public void warn(String message) {
        warnWithoutProgressMessage(message);
        sendProgressMessage(message, ProgressMessageType.WARNING);
    }

    public void debug(String pattern, Object... arguments) {
        debug(MessageFormat.format(pattern, arguments));
    }

    public void debug(String message) {
        simpleStepLogger.debug(message);
        getProcessLogger().debug(getPrefix(simpleStepLogger) + message);
    }

    public void trace(String pattern, Object... arguments) {
        trace(MessageFormat.format(pattern, arguments));
    }

    public void trace(String message) {
        simpleStepLogger.trace(message);
        getProcessLogger().trace(getPrefix(simpleStepLogger) + message);
    }

    private static String getExtendedMessage(String message, Exception e) {
        return message + ": " + e.getMessage();
    }

    private void sendProgressMessage(String message, ProgressMessageType type) {
        try {
            progressMessageService.add(new ProgressMessage(StepsUtil.getCorrelationId(context), StepsUtil.getIndexedStepName(context), type,
                message, new Timestamp(System.currentTimeMillis())));
        } catch (SLException e) {
            getProcessLogger().error(e);
        }
    }

    private org.apache.log4j.Logger getProcessLogger() {
        return processLoggerProviderFactory.getDefaultLoggerProvider().getLogger(StepsUtil.getCorrelationId(context), PARENT_LOGGER);
    }

    private static String getPrefix(Logger logger) {
        String name = logger.getName();
        return "[" + name.substring(name.lastIndexOf('.') + 1) + "] ";
    }

    @Component
    public static class Factory {

        public StepLogger create(DelegateExecution context, ProgressMessageService progressMessageService,
            ProcessLoggerProviderFactory processLoggerProviderFactory, Logger logger) {
            return new StepLogger(context, progressMessageService, processLoggerProviderFactory, logger);
        }

    }

}
