package com.sap.cloud.lm.sl.cf.core.model;

import java.text.MessageFormat;

import com.sap.cloud.lm.sl.cf.core.Messages;

public enum TypedServiceOperationState {

    DONE, FAILED, CREATING, UPDATING, DELETING;

    public static TypedServiceOperationState fromServiceOperation(ServiceOperation serviceOperation) {
        switch (serviceOperation.getState()) {
            case FAILED:
                return FAILED;
            case SUCCEEDED:
                return DONE;
            case IN_PROGRESS:
                return fromOngoingServiceOperation(serviceOperation);
            default:
                throw new IllegalStateException(MessageFormat.format(Messages.ILLEGAL_SERVICE_OPERATION_STATE,
                                                                     serviceOperation.getState()));
        }
    }

    private static TypedServiceOperationState fromOngoingServiceOperation(ServiceOperation serviceOperation) {
        switch (serviceOperation.getType()) {
            case CREATE:
                return CREATING;
            case UPDATE:
                return UPDATING;
            case DELETE:
                return DELETING;
            default:
                throw new IllegalStateException(MessageFormat.format(Messages.ILLEGAL_SERVICE_OPERATION_TYPE, serviceOperation.getType()));
        }
    }

}
