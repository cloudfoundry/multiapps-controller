package com.sap.cloud.lm.sl.cf.core.cf.services;

import java.text.MessageFormat;

import com.sap.cloud.lm.sl.cf.core.message.Messages;

public enum ServiceOperationType {

    CREATE, UPDATE, DELETE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public static ServiceOperationType fromString(String value) {
        for (ServiceOperationType type : ServiceOperationType.values()) {
            if (type.toString()
                    .equals(value)) {
                return type;
            }
        }
        throw new IllegalStateException(MessageFormat.format(Messages.ILLEGAL_SERVICE_OPERATION_TYPE, value));
    }

}
