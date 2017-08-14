package com.sap.cloud.lm.sl.cf.core.model;

public enum ZdmActionEnum {
    ZDM_ACTION("HDI_DEPLOY_ZDM_ACTION"), // Variable containing possible ZDM actions (phases).
    INSTALL("install"), // First phase. Installing application version 1. Can be skipped if the application version 1 is
                        // deployed in normal (non-ZDM) mode.
    START("start"), // Second phase. Deploying application version 2.
    FINALIZE("finalize"); // Last phase. Cleaning up unnecessary content from the data container.

    private String value;

    ZdmActionEnum(String value) {
        this.value = value;
    }

    public String toString() {
        return this.value;
    };
}
