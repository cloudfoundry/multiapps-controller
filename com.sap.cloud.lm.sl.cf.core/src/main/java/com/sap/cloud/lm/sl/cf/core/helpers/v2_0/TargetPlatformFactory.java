package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatform.TargetPlatformBuilder;

public class TargetPlatformFactory extends com.sap.cloud.lm.sl.cf.core.helpers.v1_0.TargetPlatformFactory {

    @Override
    public TargetPlatform create(String platformName, String platformType) {
        if (!isValidImplicitPlatformName(platformName)) {
            return null;
        }
        TargetPlatformBuilder builder = new TargetPlatformBuilder();
        builder.setName(platformName);
        builder.setType(platformType);
        builder.setParameters(createImplicitPlatformProperties(platformName));
        return builder.build();
    }

}
