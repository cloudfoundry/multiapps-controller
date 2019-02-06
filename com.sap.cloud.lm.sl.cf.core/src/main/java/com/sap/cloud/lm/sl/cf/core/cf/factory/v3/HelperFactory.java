package com.sap.cloud.lm.sl.cf.core.cf.factory.v3;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.cf.v2.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v3.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v3.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v3.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.Platform;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;

public class HelperFactory extends com.sap.cloud.lm.sl.cf.core.cf.factory.v2.HelperFactory {
    public HelperFactory(DescriptorHandler descriptorHandler) {
        super(descriptorHandler);
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor, Platform platform,
        BiFunction<String, String, String> spaceIdSupplier, ConfigurationEntryDao dao, CloudTarget cloudTarget,
        ApplicationConfiguration configuration) {
        ParametersChainBuilder v2ParameterChainBuilder = new ParametersChainBuilder(cast(deploymentDescriptor), cast(platform));
        ConfigurationFilterParser v2FilterParser = new ConfigurationFilterParser(cloudTarget, v2ParameterChainBuilder);
        return new ConfigurationReferencesResolver(dao, v2FilterParser, spaceIdSupplier, cloudTarget, configuration);
    }

    @Override
    public ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver, String deployId, UserMessageLogger stepLogger) {
        return new ApplicationCloudModelBuilder((com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor) deploymentDescriptor, configuration,
            deployedMta, systemParameters, xsPlaceholderResolver, deployId, stepLogger);
    }

    @Override
    public ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory() {
        return new ConfigurationSubscriptionFactory();
    }

    @Override
    public ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration) {
        return new com.sap.cloud.lm.sl.cf.core.cf.v3.ServicesCloudModelBuilder(deploymentDescriptor, configuration);
    }

    @Override
    public ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        return new com.sap.cloud.lm.sl.cf.core.cf.v3.ServiceKeysCloudModelBuilder(deploymentDescriptor);
    }

}
