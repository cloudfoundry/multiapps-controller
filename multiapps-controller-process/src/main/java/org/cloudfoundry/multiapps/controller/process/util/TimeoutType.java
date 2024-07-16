package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import java.time.Duration;

public enum TimeoutType {

    UPLOAD(SupportedParameters.UPLOAD_TIMEOUT, SupportedParameters.APPS_UPLOAD_TIMEOUT, Variables.APPS_UPLOAD_TIMEOUT_COMMAND_LINE_LEVEL,
        10800), STAGE(SupportedParameters.STAGE_TIMEOUT, SupportedParameters.APPS_STAGE_TIMEOUT,
            Variables.APPS_STAGE_TIMEOUT_COMMAND_LINE_LEVEL, 10800), START(SupportedParameters.START_TIMEOUT,
                SupportedParameters.APPS_START_TIMEOUT, Variables.APPS_START_TIMEOUT_COMMAND_LINE_LEVEL,
                10800), TASK(SupportedParameters.TASK_EXECUTION_TIMEOUT, SupportedParameters.APPS_TASK_EXECUTION_TIMEOUT,
                    Variables.APPS_TASK_EXECUTION_TIMEOUT_COMMAND_LINE_LEVEL, 86400);

    private final String moduleLevelParamName;
    private final String globalLevelParamName;
    private final Variable<Duration> commandLineLevelVariable;
    private final Integer maxAllowedValue;

    TimeoutType(String moduleLevelParamName, String globalLevelParamName, Variable<Duration> commandLineLevelVariable,
                Integer maxAllowedValue) {
        this.moduleLevelParamName = moduleLevelParamName;
        this.globalLevelParamName = globalLevelParamName;
        this.commandLineLevelVariable = commandLineLevelVariable;
        this.maxAllowedValue = maxAllowedValue;
    }

    public String getModuleLevelParamName() {
        return moduleLevelParamName;
    }

    public String getCommandLineAndGlobalLevelParamName() {
        return globalLevelParamName;
    }

    public Variable<Duration> getCommandLineLevelVariable() {
        return commandLineLevelVariable;
    }

    public Integer getMaxAllowedValue() {
        return maxAllowedValue;
    }
}
