package org.cloudfoundry.multiapps.controller.core.auditlogging.impl;

import org.cloudfoundry.multiapps.common.SLException;

public class AuditLogWriteException extends SLException {

    private static final long serialVersionUID = 5417802052219339920L;

    public AuditLogWriteException(Exception loggingException) {
        super(loggingException);
    }

}
