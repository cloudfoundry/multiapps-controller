package org.cloudfoundry.multiapps.controller.client.facade.domain;

public enum InstanceState {
    CRASHED, DOWN, RUNNING, STARTING, UNKNOWN;

    /**
     * Resolve from the CF v3 process {@code state} wire value (e.g. {@code RUNNING}). Takes the raw string rather than the OSS
     * {@code ProcessState} enum, so the domain no longer depends on cf-java-client. Unknown/blank values map to {@link #UNKNOWN}.
     */
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
