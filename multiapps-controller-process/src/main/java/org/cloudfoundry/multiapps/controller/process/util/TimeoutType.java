package org.cloudfoundry.multiapps.controller.process.util;

import java.time.Duration;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public enum TimeoutType {
    UPLOAD(new TimeoutParameterNames(SupportedParameters.UPLOAD_TIMEOUT, null, SupportedParameters.APPS_UPLOAD_TIMEOUT),
           Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE,
           10800,
           null),

    STAGE(new TimeoutParameterNames(SupportedParameters.STAGE_TIMEOUT, null, SupportedParameters.APPS_STAGE_TIMEOUT),
          Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE,
          10800,
          null),

    START(new TimeoutParameterNames(SupportedParameters.START_TIMEOUT, null, SupportedParameters.APPS_START_TIMEOUT),
          Variables.APPS_START_TIMEOUT_PROCESS_VARIABLE,
          10800,
          null),

    TASK(new TimeoutParameterNames(SupportedParameters.TASK_EXECUTION_TIMEOUT, null, SupportedParameters.APPS_TASK_EXECUTION_TIMEOUT),
         Variables.APPS_TASK_EXECUTION_TIMEOUT_PROCESS_VARIABLE,
         86400,
         null),

    CREATE_SERVICE(
        new TimeoutParameterNames(null, SupportedParameters.CREATE_SERVICE_TIMEOUT, SupportedParameters.SERVICES_CREATE_SERVICE_TIMEOUT),
        Variables.CREATE_SERVICE_TIMEOUT_PROCESS_VARIABLE,
        7200,
        Variables.SERVICE_TO_PROCESS),


    UPDATE_SERVICE(
        new TimeoutParameterNames(null, SupportedParameters.UPDATE_SERVICE_TIMEOUT, SupportedParameters.SERVICES_UPDATE_SERVICE_TIMEOUT),
        Variables.UPDATE_SERVICE_TIMEOUT_PROCESS_VARIABLE,
        7200,
        Variables.SERVICE_TO_PROCESS),

    BIND_SERVICE(
        new TimeoutParameterNames(null, SupportedParameters.BIND_SERVICE_TIMEOUT, SupportedParameters.SERVICES_BIND_SERVICE_TIMEOUT),
        Variables.BIND_SERVICE_TIMEOUT_PROCESS_VARIABLE,
        7200,
        Variables.SERVICE_TO_UNBIND_BIND),


    CREATE_SERVICE_KEY(new TimeoutParameterNames(null, SupportedParameters.CREATE_SERVICE_KEY_TIMEOUT,
                                                 SupportedParameters.SERVICES_CREATE_SERVICE_KEY_TIMEOUT),
                       Variables.CREATE_SERVICE_KEY_TIMEOUT_PROCESS_VARIABLE,
                       7200,
                       Variables.SERVICE_TO_PROCESS);

    private final TimeoutParameterNames parameterNames;
    private final Variable<Duration> processVariable;
    private final Integer maxAllowedValue;
    private final Variable<?> serviceContextVariable;

    TimeoutType(TimeoutParameterNames parameterNames,
                Variable<Duration> processVariable,
                Integer maxAllowedValue,
                Variable<?> serviceContextVariable) {
        this.parameterNames = parameterNames;
        this.processVariable = processVariable;
        this.maxAllowedValue = maxAllowedValue;
        this.serviceContextVariable = serviceContextVariable;
    }

    public String getModuleLevelParamName() {
        return parameterNames.moduleLevelParamName();
    }

    public String getResourceLevelParamName() {
        return parameterNames.resourceLevelParamName();
    }

    public String getGlobalLevelParamName() {
        return parameterNames.globalLevelParamName();
    }

    public Variable<Duration> getProcessVariable() {
        return processVariable;
    }

    public Integer getMaxAllowedValue() {
        return maxAllowedValue;
    }

    public Variable<?> getServiceContextVariable() {
        return serviceContextVariable;
    }

    public TimeoutScope getTimeoutScope() {
        return inferTimeoutScope();
    }

    private TimeoutScope inferTimeoutScope() {
        if (parameterNames.moduleLevelParamName() != null) {
            return TimeoutScope.MODULE;
        }
        return serviceContextVariable != null ? TimeoutScope.SERVICE : TimeoutScope.SERVICE_KEY;
    }

    public boolean isModuleScoped() {
        return getTimeoutScope() == TimeoutScope.MODULE;
    }

    public boolean isServiceScoped() {
        return getTimeoutScope() != TimeoutScope.MODULE;
    }

    public String getEntityLevelParamName() {
        return isModuleScoped() ? getModuleLevelParamName() : getResourceLevelParamName();
    }

    public enum TimeoutScope {
        MODULE,
        SERVICE,
        SERVICE_KEY
    }

    public record TimeoutParameterNames(String moduleLevelParamName, String resourceLevelParamName, String globalLevelParamName) {
    }
}
