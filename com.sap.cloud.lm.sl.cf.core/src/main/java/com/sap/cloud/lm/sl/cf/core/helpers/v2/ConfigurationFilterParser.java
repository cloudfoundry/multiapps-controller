package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;
import com.sap.cloud.lm.sl.mta.model.v2.Target;

public class ConfigurationFilterParser extends com.sap.cloud.lm.sl.cf.core.helpers.v1.ConfigurationFilterParser {

    public ConfigurationFilterParser(Platform platform, Target target, ParametersChainBuilder chainBuilder) {
        super(platform, target, chainBuilder);
    }

    @Override
    protected CloudTarget getCurrentOrgAndSpace() {
        Pair<String, String> currentOrgSpace = new OrgAndSpaceHelper((com.sap.cloud.lm.sl.mta.model.v2.Target) target,
            (com.sap.cloud.lm.sl.mta.model.v2.Platform) platform).getOrgAndSpace();

        return new CloudTarget(currentOrgSpace._1, currentOrgSpace._2);
    }

    @Override
    public Map<String, Object> getParameters(com.sap.cloud.lm.sl.mta.model.v1.Resource resource) {
        return ((Resource) resource).getParameters();
    }
}
