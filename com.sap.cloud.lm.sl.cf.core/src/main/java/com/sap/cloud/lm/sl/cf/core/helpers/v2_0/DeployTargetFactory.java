package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import com.sap.cloud.lm.sl.mta.model.v2_0.Target;
import com.sap.cloud.lm.sl.mta.model.v2_0.Target.TargetBuilder;

public class DeployTargetFactory extends com.sap.cloud.lm.sl.cf.core.helpers.v1_0.DeployTargetFactory {

    public Target create(String org, String space, String targetType) {

        TargetBuilder builder = new TargetBuilder();
        builder.setType(targetType);
        builder.setParameters(createImplicitTargetProperties(org, space));
        return builder.build();
    }
}
