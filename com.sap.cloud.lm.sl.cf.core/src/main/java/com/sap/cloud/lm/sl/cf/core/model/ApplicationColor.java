package com.sap.cloud.lm.sl.cf.core.model;

public enum ApplicationColor {

    BLUE, GREEN;

    public ApplicationColor getAlternativeColor() {
        return (this == BLUE) ? GREEN : BLUE;
    }

    public String asSuffix() {
        return "-" + toString();
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
