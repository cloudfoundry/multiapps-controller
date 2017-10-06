package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import com.sap.cloud.lm.sl.mta.model.v2_0.Target;
import com.sap.cloud.lm.sl.mta.model.v2_0.Target.TargetBuilder;

public class DeployTargetFactory extends com.sap.cloud.lm.sl.cf.core.helpers.v1_0.DeployTargetFactory {

    @Override
    public Target create(String platformName, String platformType) {
        if (!isValidImplicitPlatformName(platformName)) {
            return null;
        }
        TargetBuilder builder = new TargetBuilder();
        builder.setName(platformName);
        builder.setType(platformType);
        builder.setParameters(createImplicitTargetProperties(platformName));
        return builder.build();
    }

}
