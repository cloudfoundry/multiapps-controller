package com.sap.cloud.lm.sl.cf.core.cf.v3_1;

import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.SLException;
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
    public List<CloudServiceExtended> build(Set<String> modules) throws SLException {
        List<CloudServiceExtended> services = new ArrayList<>();
        for (Resource resource : deploymentDescriptor.getResources1_0()) {
            if (isService(resource, propertiesAccessor)) {
                ListUtil.addNonNull(services, getService(resource));
            } else {
                warnInvalidResourceType(resource);
            }
        }
        return services;
    }

    private void warnInvalidResourceType(Resource resource) {
        if (userMessageLogger == null || !(isOptional(resource))) {
            return;
        }
        userMessageLogger.warn("Optional resource \"{0}\" it will be not created because it's not a service", resource.getName());
    }
}
