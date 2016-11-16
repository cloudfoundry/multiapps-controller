package com.sap.cloud.lm.sl.cf.core.cf.factory.v3_0;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.v3.TargetPlatformDao;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v3_0.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v3_0.TargetPlatformFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v3_0.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.mta.builders.v2_0.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;

public class HelperFactory extends com.sap.cloud.lm.sl.cf.core.cf.factory.v2_0.HelperFactory {
    public HelperFactory(com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler descriptorHandler) {
        super(descriptorHandler);
    }

    @Override
    public TargetPlatformFactory getTargetPlatformFactory() {
        return new TargetPlatformFactory();
    }

    @Override
    public TargetPlatformDao getTargetPlatformDao(com.sap.cloud.lm.sl.cf.core.dao.v1.TargetPlatformDao dao1,
        com.sap.cloud.lm.sl.cf.core.dao.v2.TargetPlatformDao dao2, com.sap.cloud.lm.sl.cf.core.dao.v3.TargetPlatformDao dao3) {
        return dao3;
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
    public UserProvidedResourceResolver getUserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor,
        TargetPlatform platform, TargetPlatformType platformType) {
        return new UserProvidedResourceResolver(resourceHelper, cast(descriptor), cast(platform), cast(platformType));
    }
}
