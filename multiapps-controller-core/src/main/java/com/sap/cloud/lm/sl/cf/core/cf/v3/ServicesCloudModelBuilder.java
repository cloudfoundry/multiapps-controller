package com.sap.cloud.lm.sl.cf.core.cf.v3;

import com.sap.cloud.lm.sl.cf.core.cf.v2.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class ServicesCloudModelBuilder extends com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder {

    public ServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, PropertiesAccessor propertiesAccessor,
                                     CloudModelConfiguration configuration) {
        super(deploymentDescriptor, propertiesAccessor, configuration);
    }

    @Override
    protected boolean isOptional(Resource resource) {
        com.sap.cloud.lm.sl.mta.model.v3.Resource resourceV3 = (com.sap.cloud.lm.sl.mta.model.v3.Resource) resource;
        return resourceV3.isOptional();
    }
}
