package org.cloudfoundry.multiapps.controller.core.auditlogging.impl;

class AuditLoggingExceptionHandler implements DBAppender.ExceptionHandler {

    private Exception exception;

    @Override
    public void handleException(Exception e) {
        this.setException(e);
    }

    Exception getException() {
        return exception;
    }

    void setException(Exception exception) {
        this.exception = exception;
    }

}
