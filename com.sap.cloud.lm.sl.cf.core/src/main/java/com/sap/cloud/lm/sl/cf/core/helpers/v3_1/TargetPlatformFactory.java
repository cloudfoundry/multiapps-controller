package com.sap.cloud.lm.sl.cf.core.helpers.v3_1;

import com.sap.cloud.lm.sl.mta.model.v3_1.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v3_1.TargetPlatform.TargetPlatformBuilder;

public class TargetPlatformFactory extends com.sap.cloud.lm.sl.cf.core.helpers.v3_0.TargetPlatformFactory {

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
