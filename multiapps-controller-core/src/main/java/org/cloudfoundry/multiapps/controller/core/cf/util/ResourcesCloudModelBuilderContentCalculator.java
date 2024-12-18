package org.cloudfoundry.multiapps.controller.core.cf.util;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class ResourcesCloudModelBuilderContentCalculator implements CloudModelBuilderContentCalculator<Resource> {

    private final List<String> resourcesSpecifiedForDeployment;
    private final UserMessageLogger userMessageLogger;
    private final boolean processOnlyUserProvidedService;

    public ResourcesCloudModelBuilderContentCalculator(List<String> resourcesSpecifiedForDeployment, UserMessageLogger userMessageLogger,
                                                       boolean processOnlyUserProvidedService) {
        this.resourcesSpecifiedForDeployment = resourcesSpecifiedForDeployment;
        this.userMessageLogger = userMessageLogger;
        this.processOnlyUserProvidedService = processOnlyUserProvidedService;
    }

    @Override
    public List<Resource> calculateContentForBuilding(List<? extends Resource> elements) {
        return elements.stream()
                       .filter(this::isActive)
                       .filter(this::isResourceSpecifiedForDeployment)
                       .filter(this::isService)
                       .filter(this::shouldProcessOnlyUserProvidedService)
                       .collect(Collectors.toList());
    }

    private boolean shouldProcessOnlyUserProvidedService(Resource resource) {
        if (processOnlyUserProvidedService) {
            return CloudModelBuilderUtil.isUserProvidedService(resource);
        }
        return true;
    }

    private boolean isService(Resource resource) {
        if (!CloudModelBuilderUtil.isService(resource)) {
            warnInvalidResourceType(resource);
            return false;
        }
        return true;
    }

    private boolean isActive(Resource resource) {
        if (resource.getMajorSchemaVersion() < 3) {
            return true;
        }

        if (!resource.isActive()) {
            warnInactiveService(resource);
            return false;
        }

        return true;
    }

    private void warnInactiveService(Resource resource) {
        if (userMessageLogger != null) {
            userMessageLogger.warn(Messages.SERVICE_IS_NOT_ACTIVE, resource.getName());
        }
    }

    private void warnInvalidResourceType(Resource resource) {
        if (userMessageLogger != null && isOptional(resource)) {
            userMessageLogger.warn(Messages.OPTIONAL_RESOURCE_IS_NOT_SERVICE, resource.getName());
        }
    }

    private boolean isOptional(Resource resource) {
        if (resource.getMajorSchemaVersion() < 3) {
            return false;
        }
        return resource.isOptional();
    }

    private boolean isResourceSpecifiedForDeployment(Resource resource) {
        return resourcesSpecifiedForDeployment == null || resourcesSpecifiedForDeployment.contains(resource.getName());
    }

}
