package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;

public enum TimeoutType {
    UPLOAD, STAGE, START, TASK;

    public static String getTimeoutModuleLevelParameterName(TimeoutType timeoutType) {
        if (timeoutType.equals(UPLOAD)) {
            return SupportedParameters.UPLOAD_TIMEOUT;
        } else if (timeoutType.equals(STAGE)) {
            return SupportedParameters.STAGE_TIMEOUT;
        } else if (timeoutType.equals(START)) {
            return SupportedParameters.START_TIMEOUT;
        } else {
            return SupportedParameters.TASK_EXECUTION_TIMEOUT;
        }
    }

    public static String getTimeoutCommandLineAndGlobalLevelParameterName(TimeoutType timeoutType) {
        if (timeoutType.equals(UPLOAD)) {
            return SupportedParameters.APPS_UPLOAD_TIMEOUT;
        } else if (timeoutType.equals(STAGE)) {
            return SupportedParameters.APPS_STAGE_TIMEOUT;
        } else if (timeoutType.equals(START)) {
            return SupportedParameters.APPS_START_TIMEOUT;
        } else {
            return SupportedParameters.TASK_EXECUTION_TIMEOUT;
        }
    }
}
