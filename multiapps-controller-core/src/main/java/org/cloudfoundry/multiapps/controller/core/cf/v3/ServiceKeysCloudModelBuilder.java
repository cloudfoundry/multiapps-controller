package org.cloudfoundry.multiapps.controller.core.cf.v3;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class ServiceKeysCloudModelBuilder extends org.cloudfoundry.multiapps.controller.core.cf.v2.ServiceKeysCloudModelBuilder {

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
