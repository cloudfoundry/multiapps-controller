package com.sap.cloud.lm.sl.cf.core.helpers.v3_0;

import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ResourceTypeFinder;
import com.sap.cloud.lm.sl.mta.model.v3_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_0.Resource.ResourceBuilder;
import com.sap.cloud.lm.sl.mta.model.v3_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v3_0.TargetPlatformType;

public class UserProvidedResourceResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v2_0.UserProvidedResourceResolver {

    public UserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor, TargetPlatform platform,
        TargetPlatformType platformType) {
        super(resourceHelper, descriptor, platform, platformType);
    }

    @Override
    protected ResourceBuilder getResourceBuilder() {
        return new ResourceBuilder();
    }

}
