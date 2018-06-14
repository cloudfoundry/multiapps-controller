package com.sap.cloud.lm.sl.cf.core.cf.v3_1;

import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isActive;
import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isService;

import java.util.ArrayList;
import java.util.List;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;

public class ServicesCloudModelBuilder extends com.sap.cloud.lm.sl.cf.core.cf.v1_0.ServicesCloudModelBuilder {

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
        com.sap.cloud.lm.sl.mta.model.v3_1.Resource resourceV3 = (com.sap.cloud.lm.sl.mta.model.v3_1.Resource) resource;
        return resourceV3.isOptional();
    }

    @Override
    public List<CloudServiceExtended> build() {
        List<CloudServiceExtended> services = new ArrayList<>();
        for (Resource resource : deploymentDescriptor.getResources1_0()) {
            if (isService(resource, propertiesAccessor)) {
                if(isActive(resource)) {
                    ListUtil.addNonNull(services, getService(resource));
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
