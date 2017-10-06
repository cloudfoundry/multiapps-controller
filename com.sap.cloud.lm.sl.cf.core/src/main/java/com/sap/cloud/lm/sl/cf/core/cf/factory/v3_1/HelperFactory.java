package com.sap.cloud.lm.sl.cf.core.cf.factory.v3_1;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v3_1.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v3_1.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v3_1.DeployTargetFactory;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.mta.builders.v2_0.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public class HelperFactory extends com.sap.cloud.lm.sl.cf.core.cf.factory.v3_0.HelperFactory {

    public HelperFactory(com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler descriptorHandler) {
        super(descriptorHandler);
    }

    @Override
    public DeployTargetFactory getDeployTargetFactory() {
        return new DeployTargetFactory();
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor, Platform platform,
        Target target, BiFunction<String, String, String> spaceIdSupplier, ConfigurationEntryDao dao, CloudTarget cloudTarget) {
        ParametersChainBuilder v2ParameterChainBuilder = new ParametersChainBuilder(cast(deploymentDescriptor), cast(target),
            cast(platform));
        ConfigurationFilterParser v2FilterParser = new ConfigurationFilterParser(cast(platform), cast(target), v2ParameterChainBuilder);
        return new ConfigurationReferencesResolver(dao, v2FilterParser, spaceIdSupplier, cloudTarget);
    }

    @Override
    public ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory() {
        return new ConfigurationSubscriptionFactory();
    }

    @Override
    public ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        PropertiesAccessor propertiesAccessor, CloudModelConfiguration configuration) {
        return new com.sap.cloud.lm.sl.cf.core.cf.v3_1.ServicesCloudModelBuilder(deploymentDescriptor, propertiesAccessor, configuration);
    }
}
