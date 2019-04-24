package com.sap.cloud.lm.sl.cf.core.cf.v3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudServiceKey;

import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class ServiceKeysCloudModelBuilder extends com.sap.cloud.lm.sl.cf.core.cf.v2.ServiceKeysCloudModelBuilder {

    public ServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        super(deploymentDescriptor);
    }

    @Override
    public Map<String, List<CloudServiceKey>> build() {
        Map<String, List<CloudServiceKey>> serviceKeys = new HashMap<>();
        for (Resource resource : deploymentDescriptor.getResources()) {
            if (CloudModelBuilderUtil.isService(resource) && resource.isActive()) {
                serviceKeys.put(resource.getName(), getServiceKeysForService(resource));
            }
        }
        return serviceKeys;
    }

}
