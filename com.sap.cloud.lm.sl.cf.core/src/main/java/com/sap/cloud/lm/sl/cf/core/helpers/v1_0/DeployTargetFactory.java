package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import java.util.HashMap;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target.TargetBuilder;

public class DeployTargetFactory {

    protected static final String IMPLICIT_PLATFORM_NAME_PATTERN = "\\S+\\s+\\S+";

    /**
     * Creates an implicit deploy target with a name that matches the naming scheme {@literal 
     * "<org> <space>"}.
     */
    public Target create(String targetName, String targetType) {
        if (!isValidImplicitPlatformName(targetName)) {
            return null;
        }
        TargetBuilder builder = new TargetBuilder();
        builder.setName(targetName);
        builder.setType(targetType);
        builder.setProperties(createImplicitTargetProperties(targetName));
        return builder.build();
    }

    protected boolean isValidImplicitPlatformName(String platformName) {
        return platformName.matches(IMPLICIT_PLATFORM_NAME_PATTERN);
    }

    protected Map<String, Object> createImplicitTargetProperties(String targetName) {
        Map<String, Object> properties = new HashMap<>();

        String[] orgAndSpace = targetName.split("\\s+");
        properties.put(SupportedParameters.SPACE, orgAndSpace[1]);
        properties.put(SupportedParameters.ORG, orgAndSpace[0]);

        return properties;
    }

}
