package com.sap.cloud.lm.sl.cf.core.cf.factory.v2;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.List;

import com.sap.cloud.lm.sl.cf.core.cf.factory.HelperFactoryConstructor;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.v2.DescriptorParametersValidator;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.mergers.PlatformMerger;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Platform;

public class HelperFactory implements HelperFactoryConstructor {

    protected DescriptorHandler descriptorHandler;

    public HelperFactory(DescriptorHandler descriptorHandler) {
        this.descriptorHandler = descriptorHandler;
    }

    protected DescriptorHandler getHandler() {
        return cast(this.descriptorHandler);
    }

    @Override
    public ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, boolean prettyPrinting,
                                                                        DeployedMta deployedMta, String deployId,
                                                                        UserMessageLogger stepLogger) {
        return new ApplicationCloudModelBuilder(deploymentDescriptor, prettyPrinting, deployedMta, deployId, stepLogger);
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor,
                                                                              ConfigurationEntryService configurationEntryService,
                                                                              CloudTarget cloudTarget,
                                                                              ApplicationConfiguration configuration) {
        ParametersChainBuilder chainBuilder = new ParametersChainBuilder(deploymentDescriptor, null);
        com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser filterParser = new com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser(cloudTarget,
                                                                                                                                                             chainBuilder);
        return new ConfigurationReferencesResolver(configurationEntryService, filterParser, cloudTarget, configuration);
    }

    @Override
    public ConfigurationReferencesResolver
           getConfigurationReferencesResolver(ConfigurationEntryService configurationEntryService, ConfigurationFilterParser filterParser,
                                              CloudTarget cloudTarget, ApplicationConfiguration configuration) {
        return new ConfigurationReferencesResolver(configurationEntryService, cast(filterParser), cloudTarget, configuration);
    }

    @Override
    public DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
                                                                          List<ParameterValidator> parameterValidators) {
        return new DescriptorParametersValidator(descriptor, parameterValidators);
    }

    @Override
    public DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
                                                                          List<ParameterValidator> parameterValidators,
                                                                          boolean doNotCorrect) {
        return new DescriptorParametersValidator(descriptor, parameterValidators, doNotCorrect);
    }

    @Override
    public PlatformMerger getPlatformMerger(Platform platform) {
        return new PlatformMerger(platform, getHandler());
    }

    @Override
    public ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory() {
        return new ConfigurationSubscriptionFactory();
    }

    @Override
    public ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        return new ServicesCloudModelBuilder(deploymentDescriptor);
    }

    @Override
    public ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        return new ServiceKeysCloudModelBuilder(deploymentDescriptor);
    }

}
