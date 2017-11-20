package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

public enum ServiceType {
    MANAGED("managed-service"), USER_PROVIDED("user-provided-service"), EXISTING("existing-service");

    private String value;

    ServiceType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static ServiceType get(String value) {
        for (ServiceType v : values()) {
            if (v.value.equals(value))
                return v;
        }
        throw new IllegalArgumentException();
    }
}