package com.sap.cloud.lm.sl.cf.client.lib.domain;

public interface UploadInfo {

    public enum State {
        QUEUED, RUNNING, FAILED, FINISHED, UNKNOWN
    }

    State getUploadJobState();
}
