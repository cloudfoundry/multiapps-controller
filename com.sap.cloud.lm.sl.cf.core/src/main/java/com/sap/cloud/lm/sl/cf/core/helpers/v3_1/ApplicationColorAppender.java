package com.sap.cloud.lm.sl.cf.core.helpers.v3_1;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.mta.model.v3_1.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v3_1.RequiredDependency.RequiredDependencyBuilder;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource.ResourceBuilder;

public class ApplicationColorAppender extends com.sap.cloud.lm.sl.cf.core.helpers.v3_0.ApplicationColorAppender {

    public ApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationColor) {
        super(deployedMtaColor, applicationColor);
    }

    @Override
    protected RequiredDependencyBuilder getRequiredDependencyBuilder() {
        return new RequiredDependency.RequiredDependencyBuilder();
    }

    @Override
    protected ResourceBuilder getResourceBuilder() {
        return new ResourceBuilder();
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
