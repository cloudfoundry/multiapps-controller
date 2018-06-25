package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import java.util.HashMap;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target.Builder;

public class DeployTargetFactory {

    public Target create(String org, String space, String targetType) {

        Builder builder = new Builder();
        builder.setType(targetType);
        builder.setProperties(createImplicitTargetProperties(org, space));
        return builder.build();
    }

    protected Map<String, Object> createImplicitTargetProperties(String org, String space) {
        Map<String, Object> properties = new HashMap<>();

        properties.put(SupportedParameters.SPACE, space);
        properties.put(SupportedParameters.ORG, org);
        return properties;
    }
}
