package org.cloudfoundry.multiapps.controller.core.cf.v3;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class ServicesCloudModelBuilder extends org.cloudfoundry.multiapps.controller.core.cf.v2.ServicesCloudModelBuilder {

    public ServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, String namespace) {
        super(deploymentDescriptor, namespace);
    }

    @Override
    protected CommonServiceParameters getCommonServiceParameters(Resource resource) {
        return new CommonServiceParametersV3(resource);
    }

    static class CommonServiceParametersV3 extends CommonServiceParameters {

        CommonServiceParametersV3(Resource resource) {
            super(resource);
        }

        @Override
        protected boolean isOptional() {
            return resource.isOptional();
        }
    }

}
