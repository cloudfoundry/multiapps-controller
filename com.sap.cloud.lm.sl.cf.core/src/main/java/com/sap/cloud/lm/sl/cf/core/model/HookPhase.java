package com.sap.cloud.lm.sl.cf.core.model;

import java.text.MessageFormat;

public enum HookPhase {
    APPLICATION_BEFORE_STOP, APPLICATION_AFTER_STOP, NONE;

    public static HookPhase fromString(String hookPhase) {
        if (hookPhase.equals("application.after-stop")) {
            return HookPhase.APPLICATION_AFTER_STOP;
        }
        if (hookPhase.equals("application.before-stop")) {
            return HookPhase.APPLICATION_BEFORE_STOP;
        }

        throw new IllegalStateException(MessageFormat.format("Unsupported hook phase \"{0}\"", hookPhase));
    }

}
