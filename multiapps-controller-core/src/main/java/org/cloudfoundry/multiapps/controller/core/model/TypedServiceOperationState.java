package org.cloudfoundry.multiapps.controller.core.model;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.core.Messages;

import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

public enum TypedServiceOperationState {

    DONE, FAILED, CREATING, UPDATING, DELETING;

    public static TypedServiceOperationState fromServiceOperation(ServiceOperation serviceOperation) {
        switch (serviceOperation.getState()) {
            case FAILED:
                return FAILED;
            case SUCCEEDED:
                return DONE;
            case INITIAL:
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
