package com.sap.cloud.lm.sl.cf.core.cf.services;

public class ServiceOperation {

    private ServiceOperationType type;
    private String description;
    private ServiceOperationState state;

    public ServiceOperation(ServiceOperationType type, String description, ServiceOperationState state) {
        this.type = type;
        this.description = description;
        this.state = state;
    }

    public ServiceOperationType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public ServiceOperationState getState() {
        return state;
    }

}
