package com.sap.cloud.lm.sl.cf.core.model;

import java.text.MessageFormat;

public enum HookPhase {
    APPLICATION_BEFORE_STOP_LIVE, APPLICATION_BEFORE_STOP_IDLE, APPLICATION_AFTER_STOP, NONE;

    public static HookPhase fromString(String hookPhase) {
        if (hookPhase.equals("application.after-stop")) {
            return HookPhase.APPLICATION_AFTER_STOP;
        }

        if (hookPhase.equals("application.before-stop.idle")) {
            return HookPhase.APPLICATION_BEFORE_STOP_IDLE;
        }

        if (hookPhase.equals("application.before-stop.live")) {
            return HookPhase.APPLICATION_BEFORE_STOP_LIVE;
        }

        throw new IllegalStateException(MessageFormat.format("Unsupported hook phase \"{0}\"", hookPhase));
    }

}
