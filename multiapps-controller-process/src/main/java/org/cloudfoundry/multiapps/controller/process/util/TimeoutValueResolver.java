package org.cloudfoundry.multiapps.controller.process.util;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationAttributes;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Resource;

@Named
public class TimeoutValueResolver {

    private static final String DEFAULT_TIMEOUT = "default";
    private static final String SERVICE_LEVEL = "service-level";

    private final TimeoutServiceResourceNameResolver timeoutServiceResourceNameResolver;

    @FunctionalInterface
    private interface TimeoutSource {
        Optional<TimeoutResolution> resolve(ProcessContext context, TimeoutType timeoutType, StepLogger logger);
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
        this::resolveOperationParamsServiceTimeout,
        this::extractTimeoutFromServiceObject,
        this::extractTimeoutFromResourceParameters,
        this::extractTimeoutFromDescriptorParameters,
        this::resolveProcessVariableTimeout);

    @Inject
    public TimeoutValueResolver(TimeoutServiceResourceNameResolver timeoutServiceResourceNameResolver) {
        this.timeoutServiceResourceNameResolver = timeoutServiceResourceNameResolver;
    }

    public TimeoutResolution resolveTimeout(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        return getTimeoutSources(timeoutType).stream()
                                             .map(source -> source.resolve(context, timeoutType, logger))
                                             .flatMap(Optional::stream)
                                             .findFirst()
                                             .orElseGet(() -> new TimeoutResolution(timeoutType.getProcessVariable()
                                                                                               .getDefaultValue(), DEFAULT_TIMEOUT));
    }

    private Optional<TimeoutResolution> resolveProcessVariableTimeout(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        Duration processVariable = context.getVariableIfSet(timeoutType.getProcessVariable());
        if (processVariable == null) {
            return Optional.empty();
        }
        if (timeoutType.isModuleScoped() && isProcessVariableDerivedFromModuleParameter(context, timeoutType, processVariable)) {
            return Optional.empty();
        }
        return Optional.of(new TimeoutResolution(processVariable, timeoutType.getGlobalLevelParamName()));
    }

    private boolean isProcessVariableDerivedFromModuleParameter(ProcessContext context, TimeoutType timeoutType, Duration processVariable) {
        String paramName = timeoutType.getModuleLevelParamName();
        if (paramName == null) {
            return false;
        }
        return Optional.ofNullable(context.getVariableIfSet(Variables.APP_TO_PROCESS))
                       .map(CloudApplicationExtended::getName)
                       .flatMap(appName -> findModuleByAppName(getDeploymentDescriptor(context), appName))
                       .map(module -> getParameter(module.getParameters(), paramName))
                       .map(timeout -> toDuration(timeout, paramName, timeoutType.getMaxAllowedValue()))
                       .map(processVariable::equals)
                       .orElse(false);
    }

    private Optional<TimeoutResolution> resolveOperationParamsServiceTimeout(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        var operationParamsFlag = timeoutType.getOperationParamsFlag();
        if (operationParamsFlag == null || !Boolean.TRUE.equals(context.getVariableIfSet(operationParamsFlag))) {
            return Optional.empty();
        }
        return Optional.ofNullable(context.getVariableIfSet(timeoutType.getProcessVariable()))
                       .map(pv -> new TimeoutResolution(pv, timeoutType.getGlobalLevelParamName()));
    }

    private Optional<TimeoutResolution> extractTimeoutFromAppAttributes(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        String paramName = timeoutType.getModuleLevelParamName();
        if (paramName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(context.getVariable(Variables.APP_TO_PROCESS))
                       .map(app -> ApplicationAttributes.fromApplication(app, app.getEnv()))
                       .map(attrs -> attrs.get(paramName, Number.class))
                       .flatMap(value -> toResolution(value, paramName, timeoutType));
    }

    private Optional<TimeoutResolution> extractTimeoutFromModuleDescriptorParameters(ProcessContext context, TimeoutType timeoutType,
                                                                                      StepLogger logger) {
        String paramName = timeoutType.getModuleLevelParamName();
        DeploymentDescriptor descriptor = getDeploymentDescriptor(context);
        if (paramName == null || descriptor == null) {
            return Optional.empty();
        }
        String appName = Optional.ofNullable(context.getVariableIfSet(Variables.APP_TO_PROCESS))
                                 .map(CloudApplicationExtended::getName)
                                 .orElse(null);

        return findModuleByAppName(descriptor, appName)
            .or(() -> descriptor.getModules().stream().findFirst())
            .flatMap(module -> toResolution(getParameter(module.getParameters(), paramName), paramName, timeoutType));
    }

    private Optional<TimeoutResolution> extractTimeoutFromResourceParameters(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        String paramName = timeoutType.getEntityLevelParamName();
        if (paramName == null) {
            return Optional.empty();
        }
        Resource resource = timeoutServiceResourceNameResolver.resolveResource(context, timeoutType, getDeploymentDescriptor(context), logger);
        if (resource == null) {
            if (timeoutType.isServiceScoped()) {
                logger.debug(Messages.COULD_NOT_RESOLVE_DESCRIPTOR_RESOURCE_FOR_TIMEOUT_TYPE_0_PARAMETER_1, timeoutType, paramName);
            }
            return Optional.empty();
        }
        return toResolution(getParameter(resource.getParameters(), paramName), paramName, timeoutType);
    }

    private Optional<TimeoutResolution> extractTimeoutFromServiceObject(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        String paramName = Optional.ofNullable(timeoutType.getEntityLevelParamName()).orElse(SERVICE_LEVEL);
        return resolveServiceInstance(context, timeoutType)
            .flatMap(service -> Optional.ofNullable(timeoutType.getServiceTimeoutGetter())
                                        .map(getter -> getter.getServiceTimeout(service)))
            .map(timeout -> new TimeoutResolution(timeout, paramName));
    }

    private Optional<CloudServiceInstanceExtended> resolveServiceInstance(ProcessContext context, TimeoutType timeoutType) {
        if (timeoutType != TimeoutType.BIND_SERVICE) {
            return Optional.ofNullable(context.getVariableIfSet(Variables.SERVICE_TO_PROCESS));
        }
        String serviceName = context.getVariableIfSet(Variables.SERVICE_TO_UNBIND_BIND);
        List<CloudServiceInstanceExtended> services = context.getVariableIfSet(Variables.SERVICES_TO_BIND);
        if (serviceName == null || services == null) {
            return Optional.empty();
        }
        return services.stream()
                       .filter(s -> serviceName.equals(s.getName()))
                       .findFirst();
    }

    private Optional<TimeoutResolution> extractTimeoutFromDescriptorParameters(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        String paramName = timeoutType.getGlobalLevelParamName();
        DeploymentDescriptor descriptor = getDeploymentDescriptor(context);
        if (descriptor == null) {
            if (timeoutType.isServiceScoped()) {
                logger.debug(Messages.DEPLOYMENT_DESCRIPTOR_MISSING_GLOBAL_PARAMETER_0_NOT_APPLIED_FOR_1, paramName, timeoutType);
            }
            return Optional.empty();
        }
        return toResolution(getParameter(descriptor.getParameters(), paramName), paramName, timeoutType);
    }

    private Optional<TimeoutResolution> toResolution(Object timeoutValue, String parameterName, TimeoutType timeoutType) {
        return Optional.ofNullable(toDuration(timeoutValue, parameterName, timeoutType.getMaxAllowedValue()))
                       .map(duration -> new TimeoutResolution(duration, parameterName));
    }

    private Object getParameter(Map<String, Object> parameters, String parameterName) {
        return parameters != null ? parameters.get(parameterName) : null;
    }

    protected Duration toDuration(Object timeout, String timeoutParameterName, Integer maxAllowedValue) {
        if (timeout == null) {
            return null;
        }
        if (!(timeout instanceof Number number)) {
            throw new ContentException(Messages.PARAMETER_0_MUST_BE_POSITIVE_WITH_MAX_VALUE_1, timeoutParameterName, maxAllowedValue);
        }
        int value = number.intValue();
        if (value < 0 || value > maxAllowedValue) {
            throw new ContentException(Messages.PARAMETER_0_MUST_BE_POSITIVE_WITH_MAX_VALUE_1, timeoutParameterName, maxAllowedValue);
        }
        return Duration.ofSeconds(value);
    }

    private Optional<Module> findModuleByAppName(DeploymentDescriptor descriptor, String appName) {
        if (descriptor == null || appName == null) {
            return Optional.empty();
        }
        return descriptor.getModules()
                         .stream()
                         .filter(m -> appName.equals(m.getName()))
                         .findFirst();
    }

    private List<TimeoutSource> getTimeoutSources(TimeoutType timeoutType) {
        return timeoutType.isModuleScoped() ? moduleTimeoutSources : serviceTimeoutSources;
    }

    public DeploymentDescriptor getDeploymentDescriptor(ProcessContext context, StepLogger stepLogger) {
        return Stream.of(Variables.DEPLOYMENT_DESCRIPTOR,
                         Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS,
                         Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR)
                     .map(context::getVariable)
                     .filter(Objects::nonNull)
                     .findFirst()
                     .orElseGet(() -> {
                         if (stepLogger != null) {
                             stepLogger.debug(Messages.NO_DEPLOYMENT_DESCRIPTOR_FOUND_IN_CONTEXT);
                         }
                         return null;
                     });
    }

    private DeploymentDescriptor getDeploymentDescriptor(ProcessContext context) {
        return getDeploymentDescriptor(context, null);
    }
}
