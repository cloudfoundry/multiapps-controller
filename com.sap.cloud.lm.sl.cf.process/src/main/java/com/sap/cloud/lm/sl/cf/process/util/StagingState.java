package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.domain.PackageState;

public class StagingState {

    private PackageState state;
    private String error;

    public StagingState(PackageState state, String error) {
        this.state = state;
        this.error = error;
    }

    public PackageState getState() {
        return state;
    }

    public void setState(PackageState state) {
        this.state = state;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

}
