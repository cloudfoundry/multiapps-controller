package com.sap.cloud.lm.sl.cf.core.helpers.v3;

import com.sap.cloud.lm.sl.cf.core.helpers.v2.ResourceTypeFinder;
import com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3.Platform;
import com.sap.cloud.lm.sl.mta.model.v3.Resource;

public class UserProvidedResourceResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v2.UserProvidedResourceResolver {

    public UserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor, Platform platform) {
        super(resourceHelper, descriptor, platform);
    }

    @Override
    protected Resource.Builder getResourceBuilder() {
        return new Resource.Builder();
    }

}
