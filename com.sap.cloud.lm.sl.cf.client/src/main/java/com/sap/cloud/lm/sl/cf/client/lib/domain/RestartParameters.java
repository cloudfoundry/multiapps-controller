package com.sap.cloud.lm.sl.cf.client.lib.domain;

public class RestartParameters {
    private boolean shouldRestartOnVcapAppChange;
    private boolean shouldRestartOnVcapServicesChange;
    private boolean shouldRestartOnUserProvidedChange;

    public RestartParameters(boolean shouldRestartOnVcapAppChange, boolean shouldRestartOnVcapServicesChange,
        boolean shouldRestartOnUserProvidedChange) {
        this.shouldRestartOnVcapAppChange = shouldRestartOnVcapAppChange;
        this.shouldRestartOnVcapServicesChange = shouldRestartOnVcapServicesChange;
        this.shouldRestartOnUserProvidedChange = shouldRestartOnUserProvidedChange;
    }

    public boolean getShouldRestartOnVcapAppChange() {
        return shouldRestartOnVcapAppChange;
    }

    public void setShouldRestartOnVcapAppChange(boolean shouldRestartOnVcapAppChange) {
        this.shouldRestartOnVcapAppChange = shouldRestartOnVcapAppChange;
    }

    public boolean getShouldRestartOnVcapServicesChange() {
        return shouldRestartOnVcapServicesChange;
    }

    public void setShouldRestartOnVcapServicesChange(boolean shouldRestartOnVcapServicesChange) {
        this.shouldRestartOnVcapServicesChange = shouldRestartOnVcapServicesChange;
    }

    public boolean getShouldRestartOnUserProvidedChange() {
        return shouldRestartOnUserProvidedChange;
    }

    public void setShouldRestartOnUserProvidedChange(boolean shouldRestartOnUserProvidedChange) {
        this.shouldRestartOnUserProvidedChange = shouldRestartOnUserProvidedChange;
    }

}
