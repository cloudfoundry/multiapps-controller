package com.sap.cloud.lm.sl.cf.core.helpers.v3_1;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.mta.model.v3_1.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource;

public class ApplicationColorAppender extends com.sap.cloud.lm.sl.cf.core.helpers.v3_0.ApplicationColorAppender {

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

    @Override
    protected int getMinorSchemaVersion() {
        return 1;
    }

}
