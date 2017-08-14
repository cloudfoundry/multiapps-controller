package com.sap.cloud.lm.sl.cf.core.helpers.v3_0;

import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ResourceTypeFinder;
import com.sap.cloud.lm.sl.mta.model.v3_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_0.Resource.ResourceBuilder;
import com.sap.cloud.lm.sl.mta.model.v3_0.Target;
import com.sap.cloud.lm.sl.mta.model.v3_0.Platform;

public class UserProvidedResourceResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v2_0.UserProvidedResourceResolver {

    public UserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor, Target target,
        Platform platform) {
        super(resourceHelper, descriptor, target, platform);
    }

    @Override
    protected ResourceBuilder getResourceBuilder() {
        return new ResourceBuilder();
    }

}
