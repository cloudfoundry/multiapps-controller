package com.sap.cloud.lm.sl.cf.client.lib.domain;

public class RestartParameters {

    private boolean shouldRestartOnVcapAppChange;
    private boolean shouldRestartOnVcapServicesChange;
    private boolean shouldRestartOnUserProvidedChange;

    // Required by Jackson.
    protected RestartParameters() {
    }

    public RestartParameters(boolean shouldRestartOnVcapAppChange, boolean shouldRestartOnVcapServicesChange,
        boolean shouldRestartOnUserProvidedChange) {
        this.shouldRestartOnVcapAppChange = shouldRestartOnVcapAppChange;
        this.shouldRestartOnVcapServicesChange = shouldRestartOnVcapServicesChange;
        this.shouldRestartOnUserProvidedChange = shouldRestartOnUserProvidedChange;
    }

    public boolean getShouldRestartOnVcapAppChange() {
        return shouldRestartOnVcapAppChange;
    }

    public boolean getShouldRestartOnVcapServicesChange() {
        return shouldRestartOnVcapServicesChange;
    }

    public boolean getShouldRestartOnUserProvidedChange() {
        return shouldRestartOnUserProvidedChange;
    }

}
