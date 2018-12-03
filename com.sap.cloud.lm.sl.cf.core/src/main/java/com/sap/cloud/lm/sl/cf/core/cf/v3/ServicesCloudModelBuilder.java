package com.sap.cloud.lm.sl.cf.core.cf.v3;

import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isActive;
import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isService;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v1.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class ServicesCloudModelBuilder extends com.sap.cloud.lm.sl.cf.core.cf.v1.ServicesCloudModelBuilder {

    public ServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, PropertiesAccessor propertiesAccessor,
        CloudModelConfiguration configuration) {
        super(deploymentDescriptor, propertiesAccessor, configuration);
    }

    public ServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, PropertiesAccessor propertiesAccessor,
        CloudModelConfiguration configuration, UserMessageLogger userMessageLogger) {
        super(deploymentDescriptor, propertiesAccessor, configuration, userMessageLogger);
    }

    @Override
    protected boolean isOptional(Resource resource) {
        com.sap.cloud.lm.sl.mta.model.v3.Resource resourceV3 = (com.sap.cloud.lm.sl.mta.model.v3.Resource) resource;
        return resourceV3.isOptional();
    }

    @Override
    public List<CloudServiceExtended> build() {
        List<CloudServiceExtended> services = new ArrayList<>();
        for (Resource resource : deploymentDescriptor.getResources()) {
            if (isService(resource, propertiesAccessor)) {
                if(isActive(resource)) {
                    CollectionUtils.addIgnoreNull(services, getService(resource));
                } else {
                    warnInactiveService(resource);
                }
            } else {
                warnInvalidResourceType(resource);
            }
        }
        return services;
    }
    
    private void warnInactiveService(Resource resource) {
        if(userMessageLogger == null) {
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
}
