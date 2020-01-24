package com.sap.cloud.lm.sl.cf.core.cf.services;

import java.util.Map;

import org.apache.commons.collections4.MapUtils;

public class ServiceOperation {

    public static final String LAST_SERVICE_OPERATION = "last_operation";
    public static final String SERVICE_OPERATION_TYPE = "type";
    public static final String SERVICE_OPERATION_STATE = "state";
    public static final String SERVICE_OPERATION_DESCRIPTION = "description";

    private ServiceOperationType type;
    private String description;
    private ServiceOperationState state;

    ServiceOperation() {
        // Required by Jackson.
    }

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

    public static ServiceOperation fromMap(Map<String, Object> serviceOperation) {
        ServiceOperationType type = ServiceOperationType.fromString(MapUtils.getString(serviceOperation, SERVICE_OPERATION_TYPE));
        ServiceOperationState state = ServiceOperationState.fromString(MapUtils.getString(serviceOperation, SERVICE_OPERATION_STATE));
        String description = MapUtils.getString(serviceOperation, SERVICE_OPERATION_DESCRIPTION);
        return new ServiceOperation(type, description, state);
    }

    @Override
    public String toString() {
        return String.format("%s %s", type, state);
    }

}
