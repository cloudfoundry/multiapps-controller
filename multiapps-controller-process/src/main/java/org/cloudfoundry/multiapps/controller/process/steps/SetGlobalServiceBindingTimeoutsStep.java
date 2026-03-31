package org.cloudfoundry.multiapps.controller.process.steps;

import java.time.Duration;
import java.util.Map;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutValueResolver;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("setGlobalServiceBindingTimeoutsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SetGlobalServiceBindingTimeoutsStep extends SyncFlowableStep {

    private final TimeoutValueResolver timeoutValueResolver = new TimeoutValueResolver();

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug("Resolving global bind/unbind service timeout parameters");

        DeploymentDescriptor descriptor = timeoutValueResolver.getDeploymentDescriptor(context, getStepLogger());
        if (descriptor == null || descriptor.getParameters() == null) {
            getStepLogger().debug("No deployment descriptor parameters available; skipping global bind/unbind timeout setup");
            return StepPhase.DONE;
        }

        Map<String, Object> parameters = descriptor.getParameters();
        setGlobalTimeoutIfPresent(context, parameters, new TimeoutConfig(SupportedParameters.SERVICES_BIND_SERVICE_TIMEOUT,
                                                                         Variables.BIND_SERVICE_TIMEOUT_PROCESS_VARIABLE,
                                                                         TimeoutType.BIND_SERVICE.getMaxAllowedValue()));
        setGlobalTimeoutIfPresent(context, parameters, new TimeoutConfig(SupportedParameters.SERVICES_UNBIND_SERVICE_TIMEOUT,
                                                                         Variables.UNBIND_SERVICE_TIMEOUT_PROCESS_VARIABLE,
                                                                         TimeoutType.UNBIND_SERVICE.getMaxAllowedValue()));

        getStepLogger().debug("Finished resolving global bind/unbind service timeout parameters");
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return "Error while setting global bind/unbind service timeouts";
    }

    private void setGlobalTimeoutIfPresent(ProcessContext context, Map<String, Object> parameters, TimeoutConfig config) {
        Duration timeout = timeoutValueResolver.toDuration(parameters.get(config.parameterName), config.parameterName,
                                                           config.maxAllowedValue);
        if (timeout != null) {
            context.setVariable(config.targetVariable, timeout);
            getStepLogger().info("Applied global timeout parameter {0} with value {1}s to process variable {2}",
                                 config.parameterName, timeout.toSeconds(), config.targetVariable.getName());
        } else {
            getStepLogger().debug("Global timeout parameter {0} is not set", config.parameterName);
        }
    }

    private record TimeoutConfig(String parameterName, Variable<Duration> targetVariable, int maxAllowedValue) {
    }
}

