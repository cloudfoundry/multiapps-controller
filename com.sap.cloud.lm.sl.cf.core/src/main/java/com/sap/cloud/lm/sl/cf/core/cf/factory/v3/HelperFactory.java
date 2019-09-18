package com.sap.cloud.lm.sl.cf.core.cf.factory.v3;

import com.sap.cloud.lm.sl.cf.core.cf.v2.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v3.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v3.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v3.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

public class HelperFactory extends com.sap.cloud.lm.sl.cf.core.cf.factory.v2.HelperFactory {
    public HelperFactory(DescriptorHandler descriptorHandler) {
        super(descriptorHandler);
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor,
                                                                              ConfigurationEntryService configurationEntryService,
                                                                              CloudTarget cloudTarget,
                                                                              ApplicationConfiguration configuration) {
        ParametersChainBuilder v2ParameterChainBuilder = new ParametersChainBuilder(deploymentDescriptor, null);
        ConfigurationFilterParser v2FilterParser = new ConfigurationFilterParser(cloudTarget, v2ParameterChainBuilder);
        return new ConfigurationReferencesResolver(configurationEntryService, v2FilterParser, cloudTarget, configuration);
    }

    @Override
    public ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, boolean prettyPrinting,
                                                                        DeployedMta deployedMta, String deployId,
                                                                        UserMessageLogger stepLogger) {
        return new ApplicationCloudModelBuilder(deploymentDescriptor, prettyPrinting, deployedMta, deployId, stepLogger);
    }

    @Override
    public ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory() {
        return new ConfigurationSubscriptionFactory();
    }

    @Override
    public ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        return new com.sap.cloud.lm.sl.cf.core.cf.v3.ServicesCloudModelBuilder(deploymentDescriptor);
    }

    @Override
    public ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        return new com.sap.cloud.lm.sl.cf.core.cf.v3.ServiceKeysCloudModelBuilder(deploymentDescriptor);
    }

}
