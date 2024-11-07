package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;

import jakarta.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableProgressMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage.ProgressMessageType;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogger;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;

/**
 * The purpose of this class is to group logging of progress messages and process logs in a single place.
 *
 */
public class StepLogger implements UserMessageLogger {

    protected final DelegateExecution execution;
    protected final ProgressMessageService progressMessageService;
    protected final ProcessLoggerProvider processLoggerProvider;
    protected final Logger simpleStepLogger;

    public StepLogger(DelegateExecution execution, ProgressMessageService progressMessageService,
                      ProcessLoggerProvider processLoggerProvider, Logger simpleStepLogger) {
        this.execution = execution;
        this.progressMessageService = progressMessageService;
        this.processLoggerProvider = processLoggerProvider;
        this.simpleStepLogger = simpleStepLogger;
    }

    public void logFlowableTask() {
        debug(Messages.EXECUTING_TASK, execution.getCurrentActivityId(), execution.getId());
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

    public void warn(String message, Exception e, String tail) {
        warnWithoutProgressMessage(message);
        sendProgressMessage(getExtendedMessageWithTail(message, e, tail), ProgressMessageType.WARNING);
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

    private static String getExtendedMessageWithTail(String message, Exception e, String tail) {
        return message + ": " + e.getMessage() + ": " + tail;
    }

    private void sendProgressMessage(String message, ProgressMessageType type) {
        try {
            String taskId = VariableHandling.get(execution, Variables.TASK_ID);
            progressMessageService.add(ImmutableProgressMessage.builder()
                                                               .processId(VariableHandling.get(execution, Variables.CORRELATION_ID))
                                                               .taskId(taskId)
                                                               .type(type)
                                                               .text(message)
                                                               .build());

        } catch (SLException e) {
            getProcessLogger().error(e);
        }
    }

    public ProcessLogger getProcessLogger() {
        return processLoggerProvider.getLogger(execution);
    }

    public ProcessLoggerProvider getProcessLoggerProvider() {
        return processLoggerProvider;
    }

    private static String getPrefix(Logger logger) {
        String name = logger.getName();
        return "[" + name.substring(name.lastIndexOf('.') + 1) + "] ";
    }

    @Named
    public static class Factory {

        public StepLogger create(DelegateExecution execution, ProgressMessageService progressMessageService,
                                 ProcessLoggerProvider processLoggerProvider, Logger logger) {
            return new StepLogger(execution, progressMessageService, processLoggerProvider, logger);
        }

    }

}
