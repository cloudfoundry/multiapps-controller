package org.cloudfoundry.multiapps.controller.core.model;

public enum ApplicationColor {

    BLUE, GREEN;

    public ApplicationColor getAlternativeColor() {
        return (this == BLUE) ? GREEN : BLUE;
    }

    public String asSuffix() {
        return "-" + super.toString().toLowerCase();
    }

}
