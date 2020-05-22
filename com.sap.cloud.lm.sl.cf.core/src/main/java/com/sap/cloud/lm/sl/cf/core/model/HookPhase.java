package com.sap.cloud.lm.sl.cf.core.model;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum HookPhase {
    APPLICATION_BEFORE_STOP_LIVE("application.before-stop.live"),
    APPLICATION_BEFORE_STOP_IDLE("application.before-stop.idle"),
    APPLICATION_AFTER_STOP_LIVE("application.after-stop.live"),
    APPLICATION_AFTER_STOP_IDLE("application.after-stop.idle"),
    APPLICATION_BEFORE_UNMAP_ROUTES("application.before-unmap-routes"),
    APPLICATION_BEFORE_START_IDLE("application.before-start.idle"),
    APPLICATION_BEFORE_START_LIVE("application.before-start.live"),
    APPLICATION_BEFORE_START("application.before-start"),
    NONE("");

    private final String value;

    HookPhase(String value) {
        this.value = value;
    }

    private static Map<String, HookPhase> namesToValues = Arrays.stream(HookPhase.values())
                                                                .collect(Collectors.toMap(hookPhase -> hookPhase.value,
                                                                                          hookPhase -> hookPhase));

    public static HookPhase fromString(String hookPhaseName) {
        HookPhase hookPhase = namesToValues.get(hookPhaseName);
        if (hookPhase == null) {
            throw new IllegalStateException(MessageFormat.format("Unsupported hook phase \"{0}\"", hookPhaseName));
        }
        return hookPhase;
    }

    public String getValue() {
        return value;
    }
}
