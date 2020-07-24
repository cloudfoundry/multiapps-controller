package org.cloudfoundry.multiapps.controller.core.model;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum HookPhase {
    
    @Deprecated
    APPLICATION_BEFORE_STOP_LIVE("application.before-stop.live"),
    @Deprecated
    APPLICATION_BEFORE_STOP_IDLE("application.before-stop.idle"),
    @Deprecated
    APPLICATION_AFTER_STOP_LIVE("application.after-stop.live"),
    @Deprecated
    APPLICATION_AFTER_STOP_IDLE("application.after-stop.idle"),
    @Deprecated
    APPLICATION_BEFORE_UNMAP_ROUTES("application.before-unmap-routes"),


    BEFORE_STOP("before-stop"),
    DEPLOY_APPLICATION_BEFORE_STOP("deploy.application.before-stop"),
    BLUE_GREEN_APPLICATION_BEFORE_STOP_IDLE("blue-green.application.before-stop.idle"),
    BLUE_GREEN_APPLICATION_BEFORE_STOP_LIVE("blue-green.application.before-stop.live"),
    AFTER_STOP("after-stop"),
    DEPLOY_APPLICATION_AFTER_STOP("deploy.application.after-stop"),
    BLUE_GREEN_APPLICATION_AFTER_STOP_IDLE("blue-green.application.after-stop.idle"),
    BLUE_GREEN_APPLICATION_AFTER_STOP_LIVE("blue-green.application.after-stop.live"),
    BEFORE_UNMAP_ROUTES("before-unmap-routes"),
    DEPLOY_APPLICATION_BEFORE_UNMAP_ROUTES("deploy.application.before-unmap-routes"),
    BLUE_GREEN_APPLICATION_BEFORE_UNMAP_ROUTES_LIVE("blue-green.application.before-unmap-routes.live"),
    BLUE_GREEN_APPLICATION_BEFORE_UNMAP_ROUTES_IDLE("blue-green.application.before-unmap-routes.idle"),
    BEFORE_START("before-start"),
    DEPLOY_APPLICATION_BEFORE_START("deploy.application.before-start"),
    BLUE_GREEN_APPLICATION_BEFORE_START_IDLE("blue-green.application.before-start.idle"),
    BLUE_GREEN_APPLICATION_BEFORE_START_LIVE("blue-green.application.before-start.live"),
    NONE("");

    private static Map<String, HookPhase> namesToValues = Arrays.stream(HookPhase.values())
                                                                .collect(Collectors.toMap(hookPhase -> hookPhase.value,
                                                                                          Function.identity()));
    private final String value;

    HookPhase(String value) {
        this.value = value;
    }

    public static HookPhase fromString(String hookPhaseName) {
        HookPhase hookPhase = namesToValues.get(hookPhaseName);
        if (hookPhase == null) {
            throw new IllegalStateException(MessageFormat.format("Unsupported hook phase \"{0}\"", hookPhaseName));
        }
        return hookPhase;
    }
    
    @Deprecated
    public static List<HookPhase> getOldPhases() {
        return Arrays.asList(APPLICATION_BEFORE_STOP_LIVE, APPLICATION_BEFORE_STOP_IDLE, APPLICATION_AFTER_STOP_LIVE,
                             APPLICATION_AFTER_STOP_IDLE, APPLICATION_BEFORE_UNMAP_ROUTES
        );
    }

    public String getValue() {
        return value;
    }
}
