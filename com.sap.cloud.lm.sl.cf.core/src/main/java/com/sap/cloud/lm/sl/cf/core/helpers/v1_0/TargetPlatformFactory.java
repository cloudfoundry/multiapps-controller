package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import java.util.HashMap;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform.TargetPlatformBuilder;

public class TargetPlatformFactory {

    protected static final String IMPLICIT_PLATFORM_NAME_PATTERN = "\\S+\\s+\\S+";

    /**
     * Creates an implicit target platform with a name that matches the naming scheme {@literal 
     * "<org> <space>"}.
     */
    public TargetPlatform create(String platformName, String platformType) {
        if (!isValidImplicitPlatformName(platformName)) {
            return null;
        }
        TargetPlatformBuilder builder = new TargetPlatformBuilder();
        builder.setName(platformName);
        builder.setType(platformType);
        builder.setProperties(createImplicitPlatformProperties(platformName));
        return builder.build();
    }

    protected boolean isValidImplicitPlatformName(String platformName) {
        return platformName.matches(IMPLICIT_PLATFORM_NAME_PATTERN);
    }

    protected Map<String, Object> createImplicitPlatformProperties(String platformName) {
        Map<String, Object> properties = new HashMap<>();

        String[] orgAndSpace = platformName.split("\\s+");
        properties.put(SupportedParameters.SPACE, orgAndSpace[1]);
        properties.put(SupportedParameters.ORG, orgAndSpace[0]);

        return properties;
    }

}
