package com.sap.cloud.lm.sl.cf.core.model;

public enum HookPhaseProcessType {

    DEPLOY("deploy"), BLUE_GREEN_DEPLOY("blue-green");

    private final String type;

    HookPhaseProcessType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public enum HookProcessPhase {

        NONE(""), IDLE("idle"), LIVE("live");

        private final String type;

        HookProcessPhase(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

    }

}
