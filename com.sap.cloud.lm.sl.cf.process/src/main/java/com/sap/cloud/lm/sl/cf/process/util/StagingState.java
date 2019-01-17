package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.domain.PackageState;

public class StagingState {

    private PackageState state;

    private String error;

    private StagingLogs stagingLogs;
    
    public StagingState(PackageState state, String error) {
        this(state, error, null);
    }

    public StagingState(PackageState state, String error, StagingLogs stagingLogs) {
        this.state = state;
        this.error = error;
        this.stagingLogs = stagingLogs;
    }

    public StagingLogs getStagingLogs() {
        return stagingLogs;
    }

    public void setStagingLogs(StagingLogs stagingLogs) {
        this.stagingLogs = stagingLogs;
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
    
    public static class StagingLogs {
        private String logs;
        private int offset;
        
        public StagingLogs(String logs, int offset) {
            this.logs = logs;
            this.offset = offset;
        }

        public String getLogs() {
            return logs;
        }

        public void setLogs(String logs) {
            this.logs = logs;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }
        
    }

}
