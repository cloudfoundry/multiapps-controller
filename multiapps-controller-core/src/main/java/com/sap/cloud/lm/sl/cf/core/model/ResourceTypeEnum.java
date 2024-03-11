package com.sap.cloud.lm.sl.cf.core.model;

public enum ResourceTypeEnum {
    HDI_CONTAINER("com.sap.xs.hdi-container");

    private String resourceType;

    private ResourceTypeEnum(String resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public String toString() {
        return this.resourceType;
    }
}
