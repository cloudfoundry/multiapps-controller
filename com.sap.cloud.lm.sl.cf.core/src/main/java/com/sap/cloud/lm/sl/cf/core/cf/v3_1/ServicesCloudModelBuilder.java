package com.sap.cloud.lm.sl.cf.core.cf.v3_1;

import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;

public class ServicesCloudModelBuilder extends com.sap.cloud.lm.sl.cf.core.cf.v1_0.ServicesCloudModelBuilder {

    public ServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, PropertiesAccessor propertiesAccessor,
        CloudModelConfiguration configuration) {
        super(deploymentDescriptor, propertiesAccessor, configuration);
    }

    @Override
    protected boolean isOptionalResource(Resource resource) {
        com.sap.cloud.lm.sl.mta.model.v3_1.Resource resourceV3 = (com.sap.cloud.lm.sl.mta.model.v3_1.Resource) resource;
        return resourceV3.isOptional();
    }
}
