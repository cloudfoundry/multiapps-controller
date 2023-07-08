package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.flowable.engine.delegate.DelegateExecution;

public class ResourcesGetter {

    private final DelegateExecution execution;

    public ResourcesGetter(DelegateExecution execution) {
        this.execution = execution;
    }

    public List<Resource> getResources() {
        DeploymentDescriptor deploymentDescriptor = VariableHandling.get(execution, Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        List<String> resourcesSpecifiedForDeployment = VariableHandling.get(execution, Variables.RESOURCES_FOR_DEPLOYMENT);

        return deploymentDescriptor.getResources()
                                   .stream()
                                   .filter(this::isActive)
                                   .filter(r -> isResourceSpecifiedForDeployment(r, resourcesSpecifiedForDeployment))
                                   .filter(CloudModelBuilderUtil::isService)
                                   .collect(Collectors.toList());
    }

    private boolean isActive(Resource resource) {
        return resource.getMajorSchemaVersion() < 3 || resource.isActive();
    }

    private boolean isResourceSpecifiedForDeployment(Resource resource, List<String> resourcesSpecifiedForDeployment) {
        return resourcesSpecifiedForDeployment == null || resourcesSpecifiedForDeployment.contains(resource.getName());
    }

}
