package org.cloudfoundry.multiapps.controller.process.steps;

import java.time.Duration;
import java.util.Map;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("setGlobalServiceBindingTimeoutsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SetGlobalServiceBindingTimeoutsStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug("Resolving global bind/unbind service timeout parameters");
        DeploymentDescriptor descriptor = getDescriptor(context);
        if (descriptor == null || descriptor.getParameters() == null) {
            getStepLogger().debug("No deployment descriptor parameters available; skipping global bind/unbind timeout setup");
            return StepPhase.DONE;
        }

        setGlobalTimeoutIfPresent(context,
                                  descriptor.getParameters(),
                                  SupportedParameters.SERVICES_BIND_SERVICE_TIMEOUT,
                                  Variables.BIND_SERVICE_TIMEOUT_PROCESS_VARIABLE,
                                  TimeoutType.BIND_SERVICE.getMaxAllowedValue());

        setGlobalTimeoutIfPresent(context,
                                  descriptor.getParameters(),
                                  SupportedParameters.SERVICES_UNBIND_SERVICE_TIMEOUT,
                                  Variables.UNBIND_SERVICE_TIMEOUT_PROCESS_VARIABLE,
                                  TimeoutType.UNBIND_SERVICE.getMaxAllowedValue());

        getStepLogger().debug("Finished resolving global bind/unbind service timeout parameters");

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return "Error while setting global bind/unbind service timeouts";
    }

    private DeploymentDescriptor getDescriptor(ProcessContext context) {
        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        if (hasParameters(descriptor)) {
            getStepLogger().debug("Using descriptor from variable: {0}", Variables.DEPLOYMENT_DESCRIPTOR.getName());
            return descriptor;
        }
        descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
        if (hasParameters(descriptor)) {
            getStepLogger().debug("Using descriptor from variable: {0}", Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS.getName());
            return descriptor;
        }
        descriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        if (hasParameters(descriptor)) {
            getStepLogger().debug("Using descriptor from variable: {0}", Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR.getName());
            return descriptor;
        }
        getStepLogger().debug("No descriptor with parameters found in variables: {0}, {1}, {2}",
                              Variables.DEPLOYMENT_DESCRIPTOR.getName(),
                              Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS.getName(),
                              Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR.getName());
        return null;
    }

    private boolean hasParameters(DeploymentDescriptor descriptor) {
        return descriptor != null && descriptor.getParameters() != null;
    }

    private void setGlobalTimeoutIfPresent(ProcessContext context,
                                           Map<String, Object> descriptorParameters,
                                           String parameterName,
                                           Variable<Duration> targetVariable,
                                           int maxAllowedValue) {
        Object timeoutValue = descriptorParameters.get(parameterName);
        Duration timeout = toDuration(timeoutValue, parameterName, maxAllowedValue);
        if (timeout != null) {
            context.setVariable(targetVariable, timeout);
            getStepLogger().info("Applied global timeout parameter {0} with value {1}s to process variable {2}",
                                 parameterName,
                                 timeout.toSeconds(),
                                 targetVariable.getName());
        } else {
            getStepLogger().debug("Global timeout parameter {0} is not set", parameterName);
        }
    }

    private Duration toDuration(Object timeoutValue, String timeoutParameterName, int maxAllowedValue) {
        if (timeoutValue == null) {
            return null;
        }
        if (!(timeoutValue instanceof Number)) {
            throw new ContentException(Messages.PARAMETER_0_MUST_BE_POSITIVE_WITH_MAX_VALUE_1,
                                       timeoutParameterName,
                                       maxAllowedValue);
        }

        int timeout = ((Number) timeoutValue).intValue();
        if (timeout < 0 || timeout > maxAllowedValue) {
            throw new ContentException(Messages.PARAMETER_0_MUST_BE_POSITIVE_WITH_MAX_VALUE_1,
                                       timeoutParameterName,
                                       maxAllowedValue);
        }

        return Duration.ofSeconds(timeout);
    }
}

