package com.sap.cloud.lm.sl.cf.core.cf.util;

import java.util.List;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class ResourcesCloudModelBuilderContentCalculator implements CloudModelBuilderContentCalculator<Resource> {

    private List<String> resourcesSpecifiedForDeployment;
    private PropertiesAccessor propertiesAccessor;
    private UserMessageLogger userMessageLogger;

    public ResourcesCloudModelBuilderContentCalculator(List<String> resourcesSpecifiedForDeployment, PropertiesAccessor propertiesAccessor,
                                                       UserMessageLogger userMessageLogger) {
        this.resourcesSpecifiedForDeployment = resourcesSpecifiedForDeployment;
        this.propertiesAccessor = propertiesAccessor;
        this.userMessageLogger = userMessageLogger;
    }

    @Override
    public List<Resource> calculateContentForBuilding(List<? extends Resource> elements) {
        return elements.stream()
                       .filter(resource -> isActive(resource))
                       .filter(this::isResourceSpecifiedForDeployment)
                       .filter(resource -> isService(resource))
                       .collect(Collectors.toList());
    }

    private boolean isService(Resource resource) {
        if (!CloudModelBuilderUtil.isService(resource, propertiesAccessor)) {
            warnInvalidResourceType(resource);
            return false;
        }
        return true;
    }

    private boolean isActive(Resource resource) {
        if (!(resource instanceof com.sap.cloud.lm.sl.mta.model.v3.Resource)) {
            return true;
        }

        if (!CloudModelBuilderUtil.isActive(resource)) {
            warnInactiveService(resource);
            return false;
        }

        return true;
    }

    private void warnInactiveService(Resource resource) {
        if (userMessageLogger == null) {
            return;
        }
        userMessageLogger.warn(Messages.SERVICE_IS_NOT_ACTIVE, resource.getName());
    }

    private void warnInvalidResourceType(Resource resource) {
        if (userMessageLogger == null || !(isOptional(resource))) {
            return;
        }
        userMessageLogger.warn(Messages.OPTIONAL_RESOURCE_IS_NOT_SERVICE, resource.getName());
    }

    private boolean isOptional(Resource resource) {
        if (!(resource instanceof com.sap.cloud.lm.sl.mta.model.v3.Resource)) {
            return false;
        }
        com.sap.cloud.lm.sl.mta.model.v3.Resource resourceV3 = (com.sap.cloud.lm.sl.mta.model.v3.Resource) resource;
        return resourceV3.isOptional();
    }

    private boolean isResourceSpecifiedForDeployment(Resource resource) {
        return resourcesSpecifiedForDeployment.isEmpty() || resourcesSpecifiedForDeployment.contains(resource.getName());
    }

}
