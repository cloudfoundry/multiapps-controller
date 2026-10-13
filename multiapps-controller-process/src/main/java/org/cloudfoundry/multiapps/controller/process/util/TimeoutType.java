package org.cloudfoundry.multiapps.controller.process.util;

import java.time.Duration;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public enum TimeoutType {
    UPLOAD(new TimeoutParameterNames(SupportedParameters.UPLOAD_TIMEOUT, null, SupportedParameters.APPS_UPLOAD_TIMEOUT),
           Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE,
           null,
           10800,
           null),

    STAGE(new TimeoutParameterNames(SupportedParameters.STAGE_TIMEOUT, null, SupportedParameters.APPS_STAGE_TIMEOUT),
          Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE,
          null,
          10800,
          null),

    START(new TimeoutParameterNames(SupportedParameters.START_TIMEOUT, null, SupportedParameters.APPS_START_TIMEOUT),
          Variables.APPS_START_TIMEOUT_PROCESS_VARIABLE,
          null,
          10800,
          null),

    TASK(new TimeoutParameterNames(SupportedParameters.TASK_EXECUTION_TIMEOUT, null, SupportedParameters.APPS_TASK_EXECUTION_TIMEOUT),
         Variables.APPS_TASK_EXECUTION_TIMEOUT_PROCESS_VARIABLE,
         null,
         86400,
         null),

    CREATE_SERVICE(
        new TimeoutParameterNames(null, SupportedParameters.CREATE_SERVICE_TIMEOUT, SupportedParameters.SERVICES_CREATE_SERVICE_TIMEOUT),
        Variables.CREATE_SERVICE_TIMEOUT_PROCESS_VARIABLE,
        null,
        7200,
        CloudServiceInstanceExtended::getCreateServiceTimeout),

    BIND_SERVICE(
        new TimeoutParameterNames(null, SupportedParameters.BIND_SERVICE_TIMEOUT, SupportedParameters.SERVICES_BIND_SERVICE_TIMEOUT),
        Variables.BIND_SERVICE_TIMEOUT_PROCESS_VARIABLE,
        Variables.BIND_SERVICE_TIMEOUT_GLOBAL_FALLBACK,
        7200,
        CloudServiceInstanceExtended::getBindServiceTimeout),

    CREATE_SERVICE_KEY(new TimeoutParameterNames(null, SupportedParameters.CREATE_SERVICE_KEY_TIMEOUT,
                                                 SupportedParameters.SERVICES_CREATE_SERVICE_KEY_TIMEOUT),
                       Variables.CREATE_SERVICE_KEY_TIMEOUT_PROCESS_VARIABLE,
                       null,
                       7200,
                       CloudServiceInstanceExtended::getCreateServiceKeyTimeout);

    @FunctionalInterface
    public interface ServiceTimeoutGetter {
        Duration getServiceTimeout(CloudServiceInstanceExtended service);
    }

    private final TimeoutParameterNames parameterNames;
    private final Variable<Duration> processVariable;
    private final Variable<Duration> globalFallbackVariable;
    private final Integer maxAllowedValue;
    private final ServiceTimeoutGetter serviceTimeoutGetter;

    TimeoutType(TimeoutParameterNames parameterNames,
                Variable<Duration> processVariable,
                Variable<Duration> globalFallbackVariable,
                Integer maxAllowedValue,
                ServiceTimeoutGetter serviceTimeoutGetter) {
        this.parameterNames = parameterNames;
        this.processVariable = processVariable;
        this.globalFallbackVariable = globalFallbackVariable;
        this.maxAllowedValue = maxAllowedValue;
        this.serviceTimeoutGetter = serviceTimeoutGetter;
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

    public Variable<Duration> getGlobalFallbackVariable() {
        return globalFallbackVariable;
    }

    public Integer getMaxAllowedValue() {
        return maxAllowedValue;
    }

    public ServiceTimeoutGetter getServiceTimeoutGetter() {
        return serviceTimeoutGetter;
    }

    public boolean isModuleScoped() {
        return getModuleLevelParamName() != null;
    }

    public boolean isServiceScoped() {
        return !isModuleScoped();
    }

    public String getEntityLevelParamName() {
        return isModuleScoped() ? getModuleLevelParamName() : getResourceLevelParamName();
    }

    public record TimeoutParameterNames(String moduleLevelParamName, String resourceLevelParamName, String globalLevelParamName) {
    }
}
