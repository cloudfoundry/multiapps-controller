package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class ConfigurationFilterParser extends com.sap.cloud.lm.sl.cf.core.helpers.v1.ConfigurationFilterParser {

    public ConfigurationFilterParser(CloudTarget currentTarget, ParametersChainBuilder chainBuilder) {
        super(currentTarget, chainBuilder);
    }

    @Override
    public Map<String, Object> getParameters(com.sap.cloud.lm.sl.mta.model.v1.Resource resource) {
        return ((Resource) resource).getParameters();
    }
}
