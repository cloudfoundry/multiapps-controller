package com.sap.cloud.lm.sl.cf.core.cf.services;

public enum ServiceOperationType {

    CREATE("create"), UPDATE("update"), DELETE("delete");

    private final String name;

    private ServiceOperationType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static ServiceOperationType fromString(String value) {
        for (ServiceOperationType type : ServiceOperationType.values()) {
            if (type.toString().equals(value)) {
                return type;
            }
        }
        return null;
    }

}
