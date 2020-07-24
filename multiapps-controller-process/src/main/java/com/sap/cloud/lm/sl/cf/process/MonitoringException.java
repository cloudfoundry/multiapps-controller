package com.sap.cloud.lm.sl.cf.process;

import org.cloudfoundry.multiapps.common.SLException;

public class MonitoringException extends SLException {

    private static final long serialVersionUID = -4985186395675715638L;

    public MonitoringException(String message, Object... arguments) {
        super(message, arguments);
    }

    public MonitoringException(String message) {
        super(message);
    }

    public MonitoringException(Throwable cause, String message, Object... arguments) {
        super(cause, message, arguments);
    }

    public MonitoringException(Throwable cause, String message) {
        super(cause, message);
    }

    public MonitoringException(Throwable cause) {
        super(cause);
    }
}
