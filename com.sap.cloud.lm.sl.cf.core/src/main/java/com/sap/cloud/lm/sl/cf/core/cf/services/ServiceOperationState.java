package com.sap.cloud.lm.sl.cf.core.cf.services;

import java.text.MessageFormat;

import com.sap.cloud.lm.sl.cf.core.message.Messages;

public enum ServiceOperationState {

    SUCCEEDED("succeeded"), FAILED("failed"), IN_PROGRESS("in progress");

    private final String name;

    private ServiceOperationState(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static ServiceOperationState fromString(String value) {
        for (ServiceOperationState state : ServiceOperationState.values()) {
            if (state.toString().equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException(MessageFormat.format(Messages.ILLEGAL_SERVICE_OPERATION_STATE, value));
    }

}
