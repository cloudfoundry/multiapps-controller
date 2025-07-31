package org.cloudfoundry.multiapps.controller.client.facade.domain;

import org.cloudfoundry.client.v3.processes.ProcessState;

public enum InstanceState {
    CRASHED, DOWN, RUNNING, STARTING, UNKNOWN;

    public static InstanceState valueOfWithDefault(ProcessState state) {
        if (state == null) {
            return UNKNOWN;
        }
        try {
            return InstanceState.valueOf(state.getValue());
        } catch (IllegalArgumentException e) {
            return InstanceState.UNKNOWN;
        }
    }
}
