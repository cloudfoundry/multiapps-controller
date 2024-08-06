package org.cloudfoundry.multiapps.controller.persistence.services;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;

public class ProcessLogger {

    private final AbstractStringLayout layout;
    private final String activityId;
    private final String logName;
    private OperationLogEntry operationLogEntry;
    private String logMessage;

    public ProcessLogger(OperationLogEntry operationLogEntry, String logName, AbstractStringLayout layout, String activityId) {
        this.operationLogEntry = operationLogEntry;
        this.layout = layout;
        this.activityId = activityId;
        this.logName = logName;
    }

    public void info(Object message) {
        addMessageAndLogTimeToOperationLogEntry(message, ProcessLoggerMethodNamesEnum.INFO.getName());
    }

    public void debug(Object message) {
        addMessageAndLogTimeToOperationLogEntry(message, ProcessLoggerMethodNamesEnum.DEBUG.getName());
    }

    public void debug(Object message, Throwable throwable) {
        addMessageAndLogTimeToOperationLogEntry(message, ProcessLoggerMethodNamesEnum.DEBUG.getName(), throwable);
    }

    public void error(Object message) {
        addMessageAndLogTimeToOperationLogEntry(message, ProcessLoggerMethodNamesEnum.ERROR.getName());
    }

    public void error(Object message, Throwable t) {
        addMessageAndLogTimeToOperationLogEntry(message, ProcessLoggerMethodNamesEnum.ERROR.getName(), t);
    }

    public void trace(Object message) {
        addMessageAndLogTimeToOperationLogEntry(message, ProcessLoggerMethodNamesEnum.TRACE.getName());
    }

    public void warn(Object message) {
        addMessageAndLogTimeToOperationLogEntry(message, ProcessLoggerMethodNamesEnum.WARN.getName());
    }

    public void warn(Object message, Throwable t) {
        addMessageAndLogTimeToOperationLogEntry(message, ProcessLoggerMethodNamesEnum.WARN.getName(), t);
    }

    public String getLogMessage() {
        return logMessage;
    }

    public AbstractStringLayout getLayout() {
        return layout;
    }

    public String getActivityId() {
        return activityId;
    }

    public OperationLogEntry getOperationLogEntry() {
        return operationLogEntry;
    }

    private void addMessageAndLogTimeToOperationLogEntry(Object message, String methodName) {
        logMessage = layout.toSerializable(createEvent(message, methodName));
    }

    private void addMessageAndLogTimeToOperationLogEntry(Object message, String methodName, Throwable t) {
        logMessage = layout.toSerializable(createEvent(message, methodName, t));
    }

    private LogEvent createEvent(Object message, String methodName) {
        return createEvent(message, methodName, null);
    }

    private LogEvent createEvent(Object message, String methodName, Throwable t) {
        Message logMessage = new ObjectMessage(message);
        StackTraceElement stackTrace = new StackTraceElement(null, null, null, ProcessLoggerProvider.class.getName(), methodName, null, 48);
        return new Log4jLogEvent(logName, null, null, stackTrace, Level.INFO, logMessage, null, t);
    }
}
