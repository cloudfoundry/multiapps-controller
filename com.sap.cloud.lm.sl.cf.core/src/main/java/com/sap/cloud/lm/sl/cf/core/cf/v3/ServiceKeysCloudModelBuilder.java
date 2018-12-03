package com.sap.cloud.lm.sl.cf.core.cf.v3;

import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isActive;
import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.ServiceKey;

import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;


public class ServiceKeysCloudModelBuilder extends com.sap.cloud.lm.sl.cf.core.cf.v1.ServiceKeysCloudModelBuilder {

    public ServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, PropertiesAccessor propertiesAccessor) {
        super(deploymentDescriptor, propertiesAccessor);
    }
    
    @Override
    public Map<String, List<ServiceKey>> build() {
        Map<String, List<ServiceKey>> serviceKeys = new HashMap<>();
        for (Resource resource : deploymentDescriptor.getResources()) {
            if (isService(resource, propertiesAccessor) && isActive(resource)) {
                serviceKeys.put(resource.getName(), getServiceKeysForService(resource));
            }
        }
        return serviceKeys;
    }

}
