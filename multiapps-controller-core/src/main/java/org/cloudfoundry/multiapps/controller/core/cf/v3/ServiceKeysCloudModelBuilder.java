package org.cloudfoundry.multiapps.controller.core.cf.v3;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;

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
