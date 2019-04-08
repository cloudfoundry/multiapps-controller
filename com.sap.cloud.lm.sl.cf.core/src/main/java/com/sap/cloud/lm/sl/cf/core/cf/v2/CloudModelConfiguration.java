package com.sap.cloud.lm.sl.cf.core.cf.v2;

public class CloudModelConfiguration {

    protected boolean portBasedRouting;
    protected boolean prettyPrinting;

    public boolean isPortBasedRouting() {
        return portBasedRouting;
    }

    public void setPortBasedRouting(boolean portBasedRouting) {
        this.portBasedRouting = portBasedRouting;
    }

    public boolean isPrettyPrinting() {
        return prettyPrinting;
    }

    public void setPrettyPrinting(boolean prettyPrinting) {
        this.prettyPrinting = prettyPrinting;
    }

}
