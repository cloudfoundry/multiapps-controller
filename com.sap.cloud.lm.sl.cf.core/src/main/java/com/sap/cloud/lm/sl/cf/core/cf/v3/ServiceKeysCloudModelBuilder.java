package com.sap.cloud.lm.sl.cf.core.cf.v3;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        return deploymentDescriptor.getResources()
                                   .stream()
                                   .filter(CloudModelBuilderUtil::isService)
                                   .filter(Resource::isActive)
                                   .collect(Collectors.toMap(Resource::getName, this::getServiceKeysForService));
    }

}
