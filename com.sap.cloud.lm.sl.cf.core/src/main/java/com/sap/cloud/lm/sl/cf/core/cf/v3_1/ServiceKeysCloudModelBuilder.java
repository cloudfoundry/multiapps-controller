package com.sap.cloud.lm.sl.cf.core.cf.v3_1;

import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isActive;
import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.ServiceKey;

import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;

public class ServiceKeysCloudModelBuilder extends com.sap.cloud.lm.sl.cf.core.cf.v1_0.ServiceKeysCloudModelBuilder {
    
    private DeploymentDescriptor deploymentDescriptor;
    private PropertiesAccessor propertiesAccessor;

    public ServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, PropertiesAccessor propertiesAccessor) {
        super(deploymentDescriptor, propertiesAccessor);
        this.deploymentDescriptor = deploymentDescriptor;
        this.propertiesAccessor = propertiesAccessor;
    }
    
    @Override
    public Map<String, List<ServiceKey>> build() throws ContentException {
        Map<String, List<ServiceKey>> serviceKeys = new HashMap<>();
        for (Resource resource : deploymentDescriptor.getResources1_0()) {
            if (isService(resource, propertiesAccessor) && isActive(resource)) {
                serviceKeys.put(resource.getName(), getServiceKeysForService(resource));
            }
        }
        return serviceKeys;
    }

}
