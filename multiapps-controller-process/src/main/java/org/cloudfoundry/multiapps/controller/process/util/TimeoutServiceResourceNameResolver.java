package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Objects;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;

@Named
public class TimeoutServiceResourceNameResolver {

    private static final List<Variable<List<CloudServiceInstanceExtended>>> SERVICE_LIST_VARIABLES = List.of(
        Variables.SERVICES_TO_BIND,
        Variables.SERVICES_TO_CREATE,
        Variables.SERVICES_TO_POLL);

    public Resource resolveResource(ProcessContext context, TimeoutType timeoutType, DeploymentDescriptor descriptor, StepLogger logger) {
        String resourceName = resolveResourceName(context, timeoutType);
        if (resourceName == null) {
            logger.debug(Messages.COULD_NOT_RESOLVE_SERVICE_RESOURCE_NAME_FOR_TIMEOUT_TYPE_0, timeoutType);
            return null;
        }
        Resource resource = findResourceByName(descriptor, resourceName);
        if (resource == null) {
            logger.debug(Messages.COULD_NOT_FIND_RESOURCE_0_IN_DESCRIPTOR_FOR_TIMEOUT_TYPE_1, resourceName, timeoutType);
        }
        return resource;
    }

    private String resolveResourceName(ProcessContext context, TimeoutType timeoutType) {
        Variable<?> serviceContextVariable = timeoutType.getServiceContextVariable();
        if (serviceContextVariable == null) {
            return null;
        }
        Object value = context.getVariableIfSet(serviceContextVariable);
        if (value instanceof CloudServiceInstanceExtended service && service.getResourceName() != null) {
            return service.getResourceName();
        }
        String serviceName = extractServiceName(value, context);
        return serviceName != null ? findServiceResourceNameByServiceName(context, serviceName) : null;
    }

    private String extractServiceName(Object contextValue, ProcessContext context) {
        if (contextValue instanceof CloudServiceInstanceExtended service) {
            return service.getName();
        }
        if (contextValue instanceof String serviceName) {
            return serviceName;
        }
        CloudServiceKey serviceKey = context.getVariableIfSet(Variables.SERVICE_KEY_TO_PROCESS);
        if (serviceKey != null && serviceKey.getServiceInstance() != null) {
            return serviceKey.getServiceInstance().getName();
        }
        return null;
    }

    private Resource findResourceByName(DeploymentDescriptor descriptor, String resourceName) {
        if (descriptor == null || descriptor.getResources() == null) {
            return null;
        }
        return descriptor.getResources()
                         .stream()
                         .filter(resource -> resourceName.equals(resource.getName()))
                         .findFirst()
                         .orElse(null);
    }

    private String findServiceResourceNameByServiceName(ProcessContext context, String serviceName) {
        String resourceName = SERVICE_LIST_VARIABLES.stream()
                                                    .map(context::getVariableIfSet)
                                                    .filter(Objects::nonNull)
                                                    .flatMap(List::stream)
                                                    .filter(service -> serviceName.equals(service.getName())
                                                        && service.getResourceName() != null)
                                                    .map(CloudServiceInstanceExtended::getResourceName)
                                                    .findFirst()
                                                    .orElse(null);
        if (resourceName != null) {
            return resourceName;
        }
        return findServiceResourceNameInDeployedMta(context, serviceName);
    }

    private String findServiceResourceNameInDeployedMta(ProcessContext context, String serviceName) {
        DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);
        if (deployedMta == null || deployedMta.getServices() == null) {
            return null;
        }
        return deployedMta.getServices()
                          .stream()
                          .filter(service -> serviceName.equals(service.getName()) && service.getResourceName() != null)
                          .map(DeployedMtaService::getResourceName)
                          .findFirst()
                          .orElse(null);
    }
}
