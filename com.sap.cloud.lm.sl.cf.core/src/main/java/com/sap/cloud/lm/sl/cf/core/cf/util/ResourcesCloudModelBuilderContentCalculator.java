package com.sap.cloud.lm.sl.cf.core.cf.util;

import java.util.List;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class ResourcesCloudModelBuilderContentCalculator implements CloudModelBuilderContentCalculator<Resource> {

    private List<String> resourcesSpecifiedForDeployment;
    private UserMessageLogger userMessageLogger;

    public ResourcesCloudModelBuilderContentCalculator(List<String> resourcesSpecifiedForDeployment, UserMessageLogger userMessageLogger) {
        this.resourcesSpecifiedForDeployment = resourcesSpecifiedForDeployment;
        this.userMessageLogger = userMessageLogger;
    }

    @Override
    public List<Resource> calculateContentForBuilding(List<? extends Resource> elements) {
        return elements.stream()
            .filter(this::isActive)
            .filter(this::isResourceSpecifiedForDeployment)
            .filter(this::isService)
            .collect(Collectors.toList());
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
