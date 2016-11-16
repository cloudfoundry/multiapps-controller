package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import java.util.Map;

import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.builders.v2_0.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.model.v2_0.Resource;
import com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatformType;

public class ConfigurationFilterParser extends com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationFilterParser {

    public ConfigurationFilterParser(TargetPlatformType platformType, TargetPlatform platform, ParametersChainBuilder chainBuilder) {
        super(platformType, platform, chainBuilder);
    }

    @Override
    protected Pair<String, String> getCurrentOrgAndSpace() {
        return new OrgAndSpaceHelper((com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatform) platform,
            (com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatformType) platformType).getOrgAndSpace();
    }

    @Override
    public Map<String, Object> getParameters(com.sap.cloud.lm.sl.mta.model.v1_0.Resource resource) {
        return ((Resource) resource).getParameters();
    }

}
