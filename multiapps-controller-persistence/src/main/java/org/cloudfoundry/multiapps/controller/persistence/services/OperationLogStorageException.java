package org.cloudfoundry.multiapps.controller.persistence.services;

import org.cloudfoundry.multiapps.common.SLException;

public class OperationLogStorageException extends SLException {

    private static final long serialVersionUID = -4798385554251279267L;

    public OperationLogStorageException(Throwable cause) {
        super(cause);
    }

    public OperationLogStorageException(String message) {
        super(message);
    }

    public OperationLogStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
