package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;

import jakarta.inject.Named;

@Named
public class TimeoutServiceResourceNameResolver {

    public Resource resolveResource(ProcessContext context, TimeoutType timeoutType, DeploymentDescriptor descriptor, StepLogger logger) {
        String resourceName = resolveResourceName(context, timeoutType, logger);
        if (resourceName == null) {
            logger.debug("Could not resolve service resource name for timeout type {0}", timeoutType);
            return null;
        }
        Resource resource = findResourceByName(descriptor, resourceName);
        if (resource == null) {
            logger.debug("Could not find resource {0} in deployment descriptor for timeout type {1}", resourceName, timeoutType);
        }
        return resource;
    }

    private String resolveResourceName(ProcessContext context, TimeoutType timeoutType, StepLogger logger) {
        Variable<?> serviceContextVariable = timeoutType.getServiceContextVariable();
        if (serviceContextVariable == null) {
            return null;
        }

        Object value = context.getVariableIfSet(serviceContextVariable);
        if (value == null) {
            logger.debug("Service context variable {0} is missing for timeout type {1}", serviceContextVariable.getName(), timeoutType);
        }

        if (value instanceof CloudServiceInstanceExtended service) {
            if (service.getResourceName() != null) {
                return service.getResourceName();
            }
            return findServiceResourceNameByServiceName(context, service.getName());
        }

        if (value instanceof String serviceName) {
            return findServiceResourceNameByServiceName(context, serviceName);
        }

        CloudServiceKey serviceKey = context.getVariableIfSet(Variables.SERVICE_KEY_TO_PROCESS);
        if (serviceKey != null && serviceKey.getServiceInstance() != null) {
            return findServiceResourceNameByServiceName(context, serviceKey.getServiceInstance()
                                                                           .getName());
        }

        return null;
    }

    private Resource findResourceByName(DeploymentDescriptor descriptor, String resourceName) {
        if (descriptor == null || descriptor.getResources() == null || resourceName == null) {
            return null;
        }

        for (Resource resource : descriptor.getResources()) {
            if (resourceName.equals(resource.getName())) {
                return resource;
            }
        }
        return null;
    }

    private String findServiceResourceNameByServiceName(ProcessContext context, String serviceName) {
        String resourceName = findServiceResourceName(context.getVariableIfSet(Variables.SERVICES_TO_BIND), serviceName);
        if (resourceName != null) {
            return resourceName;
        }

        resourceName = findServiceResourceName(context.getVariableIfSet(Variables.SERVICES_TO_CREATE), serviceName);
        if (resourceName != null) {
            return resourceName;
        }

        resourceName = findServiceResourceName(context.getVariableIfSet(Variables.SERVICES_TO_POLL), serviceName);
        if (resourceName != null) {
            return resourceName;
        }

        return findServiceResourceNameInDeployedMta(context, serviceName);
    }

    private String findServiceResourceNameInDeployedMta(ProcessContext context, String serviceName) {
        DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);
        if (deployedMta == null || deployedMta.getServices() == null || serviceName == null) {
            return null;
        }
        for (DeployedMtaService service : deployedMta.getServices()) {
            if (serviceName.equals(service.getName()) && service.getResourceName() != null) {
                return service.getResourceName();
            }
        }
        return null;
    }

    private String findServiceResourceName(List<CloudServiceInstanceExtended> services, String serviceName) {
        if (services == null || serviceName == null) {
            return null;
        }

        for (CloudServiceInstanceExtended service : services) {
            if (serviceName.equals(service.getName()) && service.getResourceName() != null) {
                return service.getResourceName();
            }
        }
        return null;
    }
}
