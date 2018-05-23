package com.sap.cloud.lm.sl.cf.core.helpers.v3_1;

import com.sap.cloud.lm.sl.mta.model.v3_1.Target;
import com.sap.cloud.lm.sl.mta.model.v3_1.Target.TargetBuilder;

public class DeployTargetFactory extends com.sap.cloud.lm.sl.cf.core.helpers.v3_0.DeployTargetFactory {

    @Override
    public Target create(String org, String space, String targetType) {
        TargetBuilder builder = new TargetBuilder();
        builder.setType(targetType);
        builder.setParameters(createImplicitTargetProperties(org, space));
        return builder.build();
    }

}
