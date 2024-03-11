package com.sap.cloud.lm.sl.cf.core.auditlogging.impl;

import com.sap.cloud.lm.sl.common.SLException;

public class AuditLogWriteException extends SLException {

    private static final long serialVersionUID = 5417802052219339920L;

    public AuditLogWriteException(Exception loggingException) {
        super(loggingException);
    }

}
