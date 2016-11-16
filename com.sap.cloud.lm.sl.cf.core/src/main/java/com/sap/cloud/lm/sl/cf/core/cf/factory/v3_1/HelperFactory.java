package com.sap.cloud.lm.sl.cf.core.cf.factory.v3_1;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v3_1.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v3_1.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v3_1.TargetPlatformFactory;
import com.sap.cloud.lm.sl.mta.builders.v2_0.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;

public class HelperFactory extends com.sap.cloud.lm.sl.cf.core.cf.factory.v3_0.HelperFactory {

    public HelperFactory(com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler descriptorHandler) {
        super(descriptorHandler);
    }

    @Override
    public TargetPlatformFactory getTargetPlatformFactory() {
        return new TargetPlatformFactory();
    }
    
    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor,
        TargetPlatformType platformType, TargetPlatform platform, BiFunction<String, String, String> spaceIdSupplier,
        ConfigurationEntryDao dao) {
        ParametersChainBuilder v2ParameterChainBuilder = new ParametersChainBuilder(cast(deploymentDescriptor), cast(platform),
            cast(platformType));
        ConfigurationFilterParser v2FilterParser = new ConfigurationFilterParser(cast(platformType), cast(platform),
            v2ParameterChainBuilder);
        return new ConfigurationReferencesResolver(dao, v2FilterParser, spaceIdSupplier);
    }

    @Override
    public ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory() {
        return new ConfigurationSubscriptionFactory();
    }
}
