package com.sap.cloud.lm.sl.cf.core.helpers.v3;

import com.sap.cloud.lm.sl.cf.core.helpers.v1.ResourceTypeFinder;
import com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3.Platform;
import com.sap.cloud.lm.sl.mta.model.v3.Resource;
import com.sap.cloud.lm.sl.mta.model.v3.Target;

public class UserProvidedResourceResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v2.UserProvidedResourceResolver {

    public UserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor, Target target,
        Platform platform) {
        super(resourceHelper, descriptor, target, platform);
    }

    @Override
    protected Resource.Builder getResourceBuilder() {
        return new Resource.Builder();
    }

}
