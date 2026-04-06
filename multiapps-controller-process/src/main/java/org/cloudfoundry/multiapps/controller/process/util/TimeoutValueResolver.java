package org.cloudfoundry.multiapps.controller.process.util;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationAttributes;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class TimeoutValueResolver {
    private static final String DEFAULT_TIMEOUT = "default";

    private StepLogger logger;
    private final TimeoutServiceResourceNameResolver timeoutServiceResourceNameResolver = new TimeoutServiceResourceNameResolver();

    private final List<TimeoutSource> defaultTimeoutSources = List.of(this::resolveProcessVariableTimeout,
                                                                      this::extractTimeoutFromAppAttributes,
                                                                      this::extractTimeoutFromResourceParameters,
                                                                      this::extractTimeoutFromDescriptorParameters);

    private final List<TimeoutSource> serviceTimeoutSources = List.of(this::extractTimeoutFromServiceObject,
                                                                      this::extractTimeoutFromResourceParameters,
                                                                      this::resolveProcessVariableTimeout,
                                                                      this::extractTimeoutFromDescriptorParameters);

    @FunctionalInterface
    private interface TimeoutSource {
        TimeoutResolution resolve(ProcessContext context, TimeoutType timeoutType);
    }

    public record TimeoutResolution(Duration timeout, String parameterName) {
    }

    public TimeoutResolution resolveTimeout(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        this.logger = logger;
        List<TimeoutSource> timeoutSources = getTimeoutSources(timeoutType);
        for (TimeoutSource timeoutSource : timeoutSources) {
            TimeoutResolution resolvedTimeout = timeoutSource.resolve(context, timeoutType);
            if (resolvedTimeout != null) {
                return resolvedTimeout;
            }
        }
        return resolveDefaultTimeout(timeoutType);
    }

    private TimeoutResolution resolveProcessVariableTimeout(ProcessContext context, TimeoutType timeoutType) {
        return Optional.ofNullable(context.getVariableIfSet(timeoutType.getProcessVariable()))
                       .map(timeout -> new TimeoutResolution(timeout, timeoutType.getGlobalLevelParamName()))
                       .orElse(null);
    }

    private TimeoutResolution resolveDefaultTimeout(TimeoutType timeoutType) {
        return new TimeoutResolution(Duration.ofSeconds(timeoutType.getProcessVariable()
                                                                   .getDefaultValue()
                                                                   .getSeconds()),
                                     DEFAULT_TIMEOUT);
    }

    private TimeoutResolution extractTimeoutFromAppAttributes(ProcessContext context, TimeoutType timeoutType) {
        CloudApplicationExtended app = getAppToProcess(context);
        String moduleLevelParamName = timeoutType.getModuleLevelParamName();
        if (app == null || moduleLevelParamName == null) {
            return null;
        }
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app, app.getEnv());
        Object timeout = appAttributes.get(moduleLevelParamName, Number.class);
        return resolveTimeoutIfValid(timeout, moduleLevelParamName, timeoutType);
    }

    private TimeoutResolution extractTimeoutFromResourceParameters(ProcessContext context, TimeoutType timeoutType) {
        String paramName = timeoutType.getEntityLevelParamName();
        if (paramName == null) {
            return null;
        }
        DeploymentDescriptor descriptor = getDeploymentDescriptor(context);
        Resource resource = timeoutServiceResourceNameResolver.resolveResource(context, timeoutType, descriptor, this.logger);
        if (resource == null) {
            if (timeoutType.isServiceScoped()) {
                logger.warn(
                    "Could not resolve descriptor resource for timeout type {0}; resource-level timeout parameter {1} cannot be applied",
                    timeoutType,
                    paramName);
            }
            return null;
        }
        Object timeout = getResourceParameter(resource, paramName);
        return resolveTimeoutIfValid(timeout, paramName, timeoutType);
    }

    private TimeoutResolution extractTimeoutFromServiceObject(ProcessContext context, TimeoutType timeoutType) {
        CloudServiceInstanceExtended service = getServiceFromContext(context, timeoutType);
        if (service == null) {
            return null;
        }

        Duration timeout = null;
        switch (timeoutType) {
            case CREATE_SERVICE:
                timeout = service.getCreateServiceTimeout();
                break;
            case BIND_SERVICE:
                timeout = service.getBindServiceTimeout();
                break;
            case CREATE_SERVICE_KEY:
                timeout = service.getCreateServiceKeyTimeout();
                break;
            default:
                break;
        }

        if (timeout != null) {
            String paramName = timeoutType.getEntityLevelParamName();
            return new TimeoutResolution(timeout, paramName != null ? paramName : "service-level");
        }
        return null;
    }

    private CloudServiceInstanceExtended getServiceFromContext(ProcessContext context, TimeoutType timeoutType) {
        // For bind operations, look up service by name from SERVICES_TO_BIND
        if (timeoutType == TimeoutType.BIND_SERVICE) {
            String serviceName = context.getVariableIfSet(Variables.SERVICE_TO_UNBIND_BIND);
            if (serviceName != null) {
                List<CloudServiceInstanceExtended> servicesToBind = context.getVariableIfSet(Variables.SERVICES_TO_BIND);
                if (servicesToBind != null) {
                    return servicesToBind.stream()
                                        .filter(service -> serviceName.equals(service.getName()))
                                        .findFirst()
                                        .orElse(null);
                }
            }
        }
        
        // For other service operations, use SERVICE_TO_PROCESS
        return context.getVariableIfSet(Variables.SERVICE_TO_PROCESS);
    }

    private TimeoutResolution extractTimeoutFromDescriptorParameters(ProcessContext context, TimeoutType timeoutType) {
        String paramName = timeoutType.getGlobalLevelParamName();
        DeploymentDescriptor descriptor = getDeploymentDescriptor(context);
        if (descriptor == null) {
            if (timeoutType.isServiceScoped()) {
                logger.warn("Deployment descriptor is missing; global timeout parameter {0} cannot be applied for timeout type {1}",
                            paramName,
                            timeoutType);
            }
            return null;
        }
        Object timeout = getDescriptorParameter(descriptor, paramName);
        return resolveTimeoutIfValid(timeout, paramName, timeoutType);
    }

    private TimeoutResolution resolveTimeoutIfValid(Object timeoutValue, String parameterName, TimeoutType timeoutType) {
        Duration duration = toDuration(timeoutValue, parameterName, timeoutType.getMaxAllowedValue());
        if (duration == null) {
            return null;
        }
        return new TimeoutResolution(duration, parameterName);
    }

    private Object getResourceParameter(Resource resource, String parameterName) {
        return getParameter(resource != null ? resource.getParameters() : null, parameterName);
    }

    private Object getDescriptorParameter(DeploymentDescriptor descriptor, String parameterName) {
        return getParameter(descriptor != null ? descriptor.getParameters() : null, parameterName);
    }

    private Object getParameter(Map<String, Object> parameters, String parameterName) {
        return parameters != null ? parameters.get(parameterName) : null;
    }

    public Duration toDuration(Object timeout, String timeoutParameterName, Integer maxAllowedValue) {
        if (timeout == null) {
            return null;
        }
        if (!(timeout instanceof Number)) {
            throw new ContentException(Messages.PARAMETER_0_MUST_BE_POSITIVE_WITH_MAX_VALUE_1,
                                       timeoutParameterName,
                                       maxAllowedValue);
        }
        int value = toInt(timeout);
        if (value < 0 || value > maxAllowedValue) {
            throw new ContentException(Messages.PARAMETER_0_MUST_BE_POSITIVE_WITH_MAX_VALUE_1,
                                       timeoutParameterName,
                                       maxAllowedValue);
        }
        return Duration.ofSeconds(value);
    }

    private int toInt(Object number) {
        return ((Number) number).intValue();
    }

    private CloudApplicationExtended getAppToProcess(ProcessContext context) {
        return context.getVariable(Variables.APP_TO_PROCESS);
    }

    private List<TimeoutSource> getTimeoutSources(TimeoutType timeoutType) {
        return switch (timeoutType.getTimeoutScope()) {
            case MODULE -> defaultTimeoutSources;
            case SERVICE, SERVICE_KEY -> serviceTimeoutSources;
        };
    }

    public DeploymentDescriptor getDeploymentDescriptor(ProcessContext context, StepLogger stepLogger) {
        StepLogger effectiveLogger = stepLogger != null ? stepLogger : logger;
        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        if (descriptor != null) {
            return descriptor;
        }

        DeploymentDescriptor descriptorWithSystemParameters = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
        if (descriptorWithSystemParameters != null) {
            return descriptorWithSystemParameters;
        }

        DeploymentDescriptor completeDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        if (completeDescriptor != null) {
            return completeDescriptor;
        }

        if (effectiveLogger != null) {
            effectiveLogger.warn("No deployment descriptor found in context variables: {0}, {1}, {2}",
                                 Variables.DEPLOYMENT_DESCRIPTOR.getName(),
                                 Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS.getName(),
                                 Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR.getName());
        }
        return null;
    }

    private DeploymentDescriptor getDeploymentDescriptor(ProcessContext context) {
        return getDeploymentDescriptor(context, logger);
    }

}
