package org.cloudfoundry.multiapps.controller.client.facade.domain;

public enum InstanceState {
    CRASHED, DOWN, RUNNING, STARTING, UNKNOWN;
    
    public static InstanceState valueOfWithDefault(String state) {
        if (state == null) {
            return UNKNOWN;
        }
        try {
            return InstanceState.valueOf(state);
        } catch (IllegalArgumentException e) {
            return InstanceState.UNKNOWN;
        }
    }
}
