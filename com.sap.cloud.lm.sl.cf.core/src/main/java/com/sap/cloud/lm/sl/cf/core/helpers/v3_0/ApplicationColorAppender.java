package com.sap.cloud.lm.sl.cf.core.helpers.v3_0;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.mta.model.v3_0.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v3_0.Resource;

public class ApplicationColorAppender extends com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ApplicationColorAppender {

    public ApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationColor) {
        super(deployedMtaColor, applicationColor);
    }

    @Override
    protected RequiredDependency.Builder getRequiredDependencyBuilder() {
        return new RequiredDependency.Builder();
    }

    @Override
    protected Resource.Builder getResourceBuilder() {
        return new Resource.Builder();
    }

    @Override
    protected int getMajorSchemaVersion() {
        return 3;
    }

}
