package com.sap.cloud.lm.sl.cf.core.model;

public enum HdiResourceTypeEnum {
    DATA("data"), ACCESS("access"), TEMP("temp");

    private String suffix;

    private HdiResourceTypeEnum(String suffix) {
        this.suffix = suffix;
    }

    public String asSuffix() {
        return "-" + toString();
    }

    @Override
    public String toString() {
        return this.suffix;
    }
}
