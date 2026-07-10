package org.cloudfoundry.multiapps.controller.process.util;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationAttributes;
import org.cloudfoundry.multiapps.controller.core.util.DurationUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;

@Named
public class TimeoutValueResolver {

@FunctionalInterface
    private interface TimeoutResolver {
        Optional<TimeoutResolution> resolve(ProcessContext context, TimeoutType timeoutType, StepLogger logger);
    }

    public record TimeoutResolution(Duration timeout, String parameterName) {
    }

    private final List<TimeoutResolver> moduleTimeoutSources = List.of(
        this::resolveProcessVariableTimeout,
        this::extractTimeoutFromModuleDescriptorParameters,
        this::extractTimeoutFromAppAttributes,
        this::extractTimeoutFromDescriptorParameters);

    private final List<TimeoutResolver> serviceTimeoutSources = List.of(
        this::resolveProcessVariableTimeout,
        this::extractTimeoutFromServiceObject,
        this::extractTimeoutFromDescriptorParameters);

    public TimeoutResolution resolveTimeout(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        return getTimeoutSources(timeoutType).stream()
                                             .map(source -> source.resolve(context, timeoutType, logger))
                                             .flatMap(Optional::stream)
                                             .findFirst()
                                             .orElseGet(() -> new TimeoutResolution(timeoutType.getProcessVariable()
                                                                                               .getDefaultValue(), timeoutType.getGlobalLevelParamName()));
    }

    private Optional<TimeoutResolution> resolveProcessVariableTimeout(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        return Optional.ofNullable(context.getVariableIfSet(timeoutType.getProcessVariable()))
                       .map(pv -> new TimeoutResolution(pv, timeoutType.getGlobalLevelParamName()));
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
            .flatMap(module -> toResolution(getParameter(module.getParameters(), paramName), paramName, timeoutType));
    }

    private Optional<TimeoutResolution> extractTimeoutFromAppAttributes(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        String paramName = timeoutType.getModuleLevelParamName();
        if (paramName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(context.getVariable(Variables.APP_TO_PROCESS))
                       .map(app -> ApplicationAttributes.fromApplication(app, app.getEnv()))
                       .map(attrs -> attrs.get(paramName, Number.class))
                       .flatMap(value -> toResolution(value, timeoutType.getGlobalLevelParamName(), timeoutType));
    }

    private Optional<TimeoutResolution> extractTimeoutFromServiceObject(ProcessContext context, TimeoutType timeoutType,
                                                                        StepLogger logger) {
        return resolveServiceInstance(context, timeoutType)
            .flatMap(service -> Optional.ofNullable(timeoutType.getServiceTimeoutGetter())
                                        .map(getter -> getter.getServiceTimeout(service)))
            .map(timeout -> new TimeoutResolution(timeout, timeoutType.getEntityLevelParamName()));
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

    private Optional<TimeoutResolution> extractTimeoutFromDescriptorParameters(ProcessContext context, TimeoutType timeoutType,
                                                                               StepLogger logger) {
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
        return DurationUtil.parseDuration(timeout, timeoutParameterName, maxAllowedValue);
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

    private List<TimeoutResolver> getTimeoutSources(TimeoutType timeoutType) {
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
