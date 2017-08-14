package com.sap.cloud.lm.sl.cf.process.exception;

import java.text.MessageFormat;

import com.sap.cloud.lm.sl.common.SLException;

public class MonitoringException extends SLException {

    private static final long serialVersionUID = -4985186395675715638L;

    public MonitoringException(String message, Object... arguments) {
        this(null, message, arguments);
    }

    public MonitoringException(String message) {
        super(message);
    }

    public MonitoringException(Throwable cause, String message, Object... arguments) {
        super(MessageFormat.format(message, arguments), cause);
    }

    public MonitoringException(Throwable cause, String message) {
        super(message, cause);
    }

    public MonitoringException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
