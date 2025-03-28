package org.cloudfoundry.multiapps.controller.persistence.services;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;

import java.util.Objects;
import java.util.UUID;

public class ProcessLogger {

    private final AbstractStringLayout layout;
    private final String activityId;
    private final String logName;
    private final UUID id;
    private OperationLogEntry operationLogEntry;
    private String logMessage;

    private boolean headerIsSet = false;

    public ProcessLogger(OperationLogEntry operationLogEntry, String logName, AbstractStringLayout layout, String activityId) {
        this.operationLogEntry = operationLogEntry;
        this.layout = layout;
        this.activityId = activityId;
        this.logName = logName;
        this.id = UUID.randomUUID();
    }

    public void info(Object message) {
        createLogMessage(message, Level.INFO);
    }

    public void debug(Object message) {
        createLogMessage(message, Level.DEBUG);
    }

    public void debug(Object message, Throwable throwable) {
        createLogMessage(message, Level.DEBUG, throwable);
    }

    public void error(Object message) {
        createLogMessage(message, Level.ERROR);
    }

    public void error(Object message, Throwable t) {
        createLogMessage(message, Level.ERROR, t);
    }

    public void trace(Object message) {
        createLogMessage(message, Level.TRACE);
    }

    public void warn(Object message) {
        createLogMessage(message, Level.WARN);
    }

    public void warn(Object message, Throwable t) {
        createLogMessage(message, Level.WARN, t);
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

    private void createLogMessage(Object message, Level logLevel) {
        String formattedLogMessage = layout.toSerializable(createEvent((message), logLevel));
        setLogMessage(formattedLogMessage);
    }

    private void createLogMessage(Object message, Level logLevel, Throwable t) {
        String formattedLogMessage = layout.toSerializable(createEvent(message, logLevel, t));
        setLogMessage(formattedLogMessage);
    }

    private LogEvent createEvent(Object message, Level logLevel) {
        return createEvent(message, logLevel, null);
    }

    // Here we create Log4J event so we can use the method that create operation log format text, This method requires log4j event.
    // The StackTraceElement is required because there isn't a contructor that we can use without StackTraceElement
    private LogEvent createEvent(Object message, Level logLevel, Throwable t) {
        Message logMessage = new ObjectMessage(message);
        StackTraceElement stackTrace = new StackTraceElement(null, null, null, ProcessLoggerProvider.class.getName(), logLevel.name(), null,
                                                             0);
        return new Log4jLogEvent(logName, null, null, stackTrace, logLevel, logMessage, null, t);
    }

    private void setLogMessage(String formattedLogMessage) {
        byte[] header = layout.getHeader();
        if (header != null && !headerIsSet) {
            setLogMessageWithHeader(formattedLogMessage, header);
        } else {
            logMessage = formattedLogMessage;
        }
    }

    private void setLogMessageWithHeader(String formattedLogMessage, byte[] header) {
        StringBuilder builder = new StringBuilder();
        String headerString = new String(header);
        builder.append(headerString);
        builder.append(formattedLogMessage);
        logMessage = builder.toString();
        headerIsSet = true;
    }

    @Override
    public boolean equals(Object incommingObject) {
        if (this == incommingObject) {
            return true;
        }
        if (incommingObject == null || getClass() != incommingObject.getClass()) {
            return false;
        }
        ProcessLogger processLogger = (ProcessLogger) incommingObject;
        return Objects.equals(id, processLogger.id) && Objects.equals(layout, processLogger.layout) && Objects.equals(activityId,
                                                                                                                      processLogger.activityId)
            && Objects.equals(
            logName, processLogger.logName) && Objects.equals(operationLogEntry, processLogger.operationLogEntry) && Objects.equals(
            logMessage, processLogger.logMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, layout, activityId, logName, operationLogEntry, logMessage);
    }
}
