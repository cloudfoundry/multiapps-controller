package com.sap.cloud.lm.sl.cf.core.cf.v3;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class ServicesCloudModelBuilder extends com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder {

    public ServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, String namespace) {
        super(deploymentDescriptor, namespace);
    }

    @Override
    protected boolean isOptional(Resource resource) {
        return resource.isOptional();
    }

}
