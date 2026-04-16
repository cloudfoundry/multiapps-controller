package org.cloudfoundry.multiapps.controller.process.util;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationAttributes;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Resource;

@Named
public class TimeoutValueResolver {

    private static final String DEFAULT_TIMEOUT = "default";
    private static final String SERVICE_LEVEL = "service-level";

    private static final List<Variable<DeploymentDescriptor>> DESCRIPTOR_VARIABLES = List.of(
        Variables.DEPLOYMENT_DESCRIPTOR,
        Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS,
        Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);

    private final TimeoutServiceResourceNameResolver timeoutServiceResourceNameResolver;

    @FunctionalInterface
    private interface TimeoutSource {
        TimeoutResolution resolve(ProcessContext context, TimeoutType timeoutType, StepLogger logger);
    }

    public record TimeoutResolution(Duration timeout, String parameterName) {
    }

    private final List<TimeoutSource> moduleTimeoutSources = List.of(
        this::resolveProcessVariableTimeout,
        this::extractTimeoutFromModuleDescriptorParameters,
        this::extractTimeoutFromAppAttributes,
        this::extractTimeoutFromResourceParameters,
        this::extractTimeoutFromDescriptorParameters);

    private final List<TimeoutSource> serviceTimeoutSources = List.of(
        this::extractTimeoutFromServiceObject,
        this::extractTimeoutFromResourceParameters,
        this::extractTimeoutFromDescriptorParameters,
        this::resolveProcessVariableTimeout);

    @Inject
    public TimeoutValueResolver(TimeoutServiceResourceNameResolver timeoutServiceResourceNameResolver) {
        this.timeoutServiceResourceNameResolver = timeoutServiceResourceNameResolver;
    }

    public TimeoutResolution resolveTimeout(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        for (TimeoutSource source : getTimeoutSources(timeoutType)) {
            TimeoutResolution resolution = source.resolve(context, timeoutType, logger);
            if (resolution != null) {
                return resolution;
            }
        }
        return resolveDefaultTimeout(timeoutType);
    }

    private TimeoutResolution resolveProcessVariableTimeout(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        Duration processVariable = context.getVariableIfSet(timeoutType.getProcessVariable());
        if (processVariable == null) {
            return null;
        }
        if (timeoutType.isModuleScoped() && isProcessVariableDerivedFromModuleParameter(context, timeoutType, processVariable)) {
            return null;
        }
        return new TimeoutResolution(processVariable, timeoutType.getGlobalLevelParamName());
    }

    private boolean isProcessVariableDerivedFromModuleParameter(ProcessContext context, TimeoutType timeoutType, Duration processVariable) {
        String moduleLevelParamName = timeoutType.getModuleLevelParamName();
        if (moduleLevelParamName == null) {
            return false;
        }
        CloudApplicationExtended app = context.getVariableIfSet(Variables.APP_TO_PROCESS);
        if (app == null) {
            return false;
        }
        Module module = findModuleByAppName(getDeploymentDescriptor(context), app.getName());
        if (module == null) {
            return false;
        }
        Duration moduleDuration = toDuration(getParameter(module.getParameters(), moduleLevelParamName),
                                             moduleLevelParamName, timeoutType.getMaxAllowedValue());
        return processVariable.equals(moduleDuration);
    }

    private TimeoutResolution resolveDefaultTimeout(TimeoutType timeoutType) {
        Duration defaultDuration = timeoutType.getProcessVariable()
                                              .getDefaultValue();
        return new TimeoutResolution(defaultDuration, DEFAULT_TIMEOUT);
    }

    private TimeoutResolution extractTimeoutFromAppAttributes(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String moduleLevelParamName = timeoutType.getModuleLevelParamName();
        if (app == null || moduleLevelParamName == null) {
            return null;
        }
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app, app.getEnv());
        Object timeout = appAttributes.get(moduleLevelParamName, Number.class);
        return toResolution(timeout, moduleLevelParamName, timeoutType);
    }

    private TimeoutResolution extractTimeoutFromModuleDescriptorParameters(ProcessContext context, TimeoutType timeoutType,
                                                                           StepLogger logger) {
        String moduleLevelParamName = timeoutType.getModuleLevelParamName();
        if (moduleLevelParamName == null) {
            return null;
        }
        DeploymentDescriptor descriptor = getDeploymentDescriptor(context);
        if (descriptor == null) {
            return null;
        }
        CloudApplicationExtended appToProcess = context.getVariableIfSet(Variables.APP_TO_PROCESS);
        if (appToProcess != null) {
            Module matchingModule = findModuleByAppName(descriptor, appToProcess.getName());
            if (matchingModule != null) {
                TimeoutResolution resolution = resolveModuleTimeout(matchingModule, moduleLevelParamName, timeoutType);
                if (resolution != null) {
                    return resolution;
                }
            }
        }
        return descriptor.getModules()
                         .stream()
                         .map(module -> resolveModuleTimeout(module, moduleLevelParamName, timeoutType))
                         .filter(Objects::nonNull)
                         .findFirst()
                         .orElse(null);
    }

    private TimeoutResolution resolveModuleTimeout(Module module, String paramName, TimeoutType timeoutType) {
        Object timeout = getParameter(module.getParameters(), paramName);
        return timeout != null ? toResolution(timeout, paramName, timeoutType) : null;
    }

    private TimeoutResolution extractTimeoutFromResourceParameters(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        String paramName = timeoutType.getEntityLevelParamName();
        if (paramName == null) {
            return null;
        }
        DeploymentDescriptor descriptor = getDeploymentDescriptor(context);
        Resource resource = timeoutServiceResourceNameResolver.resolveResource(context, timeoutType, descriptor, logger);
        if (resource == null) {
            if (timeoutType.isServiceScoped()) {
                logger.debug(
                    "Could not resolve descriptor resource for timeout type {0}; resource-level timeout parameter {1} cannot be applied",
                    timeoutType, paramName);
            }
            return null;
        }
        return toResolution(getParameter(resource.getParameters(), paramName), paramName, timeoutType);
    }

    private TimeoutResolution extractTimeoutFromServiceObject(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        return Optional.ofNullable(resolveServiceInstance(context, timeoutType))
                       .flatMap(service -> Optional.ofNullable(timeoutType.getServiceTimeoutGetter())
                                                   .map(getter -> getter.getServiceTimeout(service)))
                       .map(timeout -> new TimeoutResolution(timeout, Optional.ofNullable(timeoutType.getEntityLevelParamName())
                                                                              .orElse(SERVICE_LEVEL)))
                       .orElse(null);
    }

    private CloudServiceInstanceExtended resolveServiceInstance(ProcessContext context, TimeoutType timeoutType) {
        if (timeoutType == TimeoutType.BIND_SERVICE) {
            return resolveBindServiceInstance(context);
        }
        return context.getVariableIfSet(Variables.SERVICE_TO_PROCESS);
    }

    private CloudServiceInstanceExtended resolveBindServiceInstance(ProcessContext context) {
        String serviceName = context.getVariableIfSet(Variables.SERVICE_TO_UNBIND_BIND);
        List<CloudServiceInstanceExtended> servicesToBind = context.getVariableIfSet(Variables.SERVICES_TO_BIND);
        if (serviceName == null || servicesToBind == null) {
            return null;
        }
        return servicesToBind.stream()
                             .filter(service -> serviceName.equals(service.getName()))
                             .findFirst()
                             .orElse(null);
    }

    private TimeoutResolution extractTimeoutFromDescriptorParameters(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        String paramName = timeoutType.getGlobalLevelParamName();
        DeploymentDescriptor descriptor = getDeploymentDescriptor(context);
        if (descriptor == null) {
            if (timeoutType.isServiceScoped()) {
                logger.debug("Deployment descriptor is missing; global timeout parameter {0} cannot be applied for timeout type {1}",
                             paramName, timeoutType);
            }
            return null;
        }
        return toResolution(getParameter(descriptor.getParameters(), paramName), paramName, timeoutType);
    }

    private TimeoutResolution toResolution(Object timeoutValue, String parameterName, TimeoutType timeoutType) {
        Duration duration = toDuration(timeoutValue, parameterName, timeoutType.getMaxAllowedValue());
        return duration != null ? new TimeoutResolution(duration, parameterName) : null;
    }

    private Object getParameter(Map<String, Object> parameters, String parameterName) {
        return parameters != null ? parameters.get(parameterName) : null;
    }

    Duration toDuration(Object timeout, String timeoutParameterName, Integer maxAllowedValue) {
        if (timeout == null) {
            return null;
        }
        if (!(timeout instanceof Number number)) {
            throw new ContentException(Messages.PARAMETER_0_MUST_BE_POSITIVE_WITH_MAX_VALUE_1,
                                       timeoutParameterName, maxAllowedValue);
        }
        int value = number.intValue();
        if (value < 0 || value > maxAllowedValue) {
            throw new ContentException(Messages.PARAMETER_0_MUST_BE_POSITIVE_WITH_MAX_VALUE_1,
                                       timeoutParameterName, maxAllowedValue);
        }
        return Duration.ofSeconds(value);
    }

    private Module findModuleByAppName(DeploymentDescriptor descriptor, String appName) {
        if (descriptor == null || appName == null) {
            return null;
        }
        return descriptor.getModules()
                         .stream()
                         .filter(m -> appName.equals(m.getName()))
                         .findFirst()
                         .orElse(null);
    }

    private List<TimeoutSource> getTimeoutSources(TimeoutType timeoutType) {
        return timeoutType.isModuleScoped() ? moduleTimeoutSources : serviceTimeoutSources;
    }

    public DeploymentDescriptor getDeploymentDescriptor(ProcessContext context, StepLogger stepLogger) {
        return DESCRIPTOR_VARIABLES.stream()
                                   .map(context::getVariable)
                                   .filter(Objects::nonNull)
                                   .findFirst()
                                   .orElseGet(() -> {
                                       if (stepLogger != null) {
                                           stepLogger.debug("No deployment descriptor found in context variables: {0}, {1}, {2}",
                                                            Variables.DEPLOYMENT_DESCRIPTOR.getName(),
                                                            Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS.getName(),
                                                            Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR.getName());
                                       }
                                       return null;
                                   });
    }

    private DeploymentDescriptor getDeploymentDescriptor(ProcessContext context) {
        return getDeploymentDescriptor(context, null);
    }
}
