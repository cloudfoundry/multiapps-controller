package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.Map;

public class CloudTaskExtended extends CloudTask {

    private Integer exitCode;

    public CloudTaskExtended(Meta meta, String name) {
        super(meta, name);
    }

    public CloudTaskExtended(Meta meta, String name, String command, Map<String, String> environmentVariables, State state, Result result,
        Integer exitCode) {
        super(meta, name, command, environmentVariables, state, result);
        this.exitCode = exitCode;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

}
