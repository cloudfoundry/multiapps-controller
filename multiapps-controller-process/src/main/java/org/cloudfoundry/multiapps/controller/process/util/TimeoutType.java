package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import java.time.Duration;

public enum TimeoutType {

    UPLOAD(SupportedParameters.UPLOAD_TIMEOUT, SupportedParameters.APPS_UPLOAD_TIMEOUT, Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE,
           10800), STAGE(SupportedParameters.STAGE_TIMEOUT, SupportedParameters.APPS_STAGE_TIMEOUT,
                         Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE, 10800), START(SupportedParameters.START_TIMEOUT,
                                                                                      SupportedParameters.APPS_START_TIMEOUT,
                                                                                      Variables.APPS_START_TIMEOUT_PROCESS_VARIABLE,
                                                                                      10800), TASK(
        SupportedParameters.TASK_EXECUTION_TIMEOUT, SupportedParameters.APPS_TASK_EXECUTION_TIMEOUT,
        Variables.APPS_TASK_EXECUTION_TIMEOUT_PROCESS_VARIABLE, 86400);

    private final String moduleLevelParamName;
    private final String globalLevelParamName;
    private final Variable<Duration> processVariable;
    private final Integer maxAllowedValue;

    TimeoutType(String moduleLevelParamName, String globalLevelParamName, Variable<Duration> processVariable, Integer maxAllowedValue) {
        this.moduleLevelParamName = moduleLevelParamName;
        this.globalLevelParamName = globalLevelParamName;
        this.processVariable = processVariable;
        this.maxAllowedValue = maxAllowedValue;
    }

    public String getModuleLevelParamName() {
        return moduleLevelParamName;
    }

    public String getProcessVariableAndGlobalLevelParamName() {
        return globalLevelParamName;
    }

    public Variable<Duration> getProcessVariable() {
        return processVariable;
    }

    public Integer getMaxAllowedValue() {
        return maxAllowedValue;
    }
}
