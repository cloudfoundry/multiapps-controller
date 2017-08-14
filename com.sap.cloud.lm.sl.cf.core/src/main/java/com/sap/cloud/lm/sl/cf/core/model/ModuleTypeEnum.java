package com.sap.cloud.lm.sl.cf.core.model;

public enum ModuleTypeEnum {

    HDI("com.sap.xs.hdi"), HDI_ZDM("com.sap.xs.hdi-zdm");

    private String moduleType;

    private ModuleTypeEnum(String moduleType) {
        this.moduleType = moduleType;
    }

    @Override
    public String toString() {
        return this.moduleType;
    }
}
