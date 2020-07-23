package com.sap.cloud.lm.sl.cf.core.cf.factory.v2;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.mta.builders.v2.ParametersChainBuilder;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandler;
import org.cloudfoundry.multiapps.mta.mergers.PlatformMerger;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;

import com.sap.cloud.lm.sl.cf.core.cf.factory.HelperFactoryConstructor;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.v2.DescriptorParametersCompatabilityValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.v2.DescriptorParametersValidator;

public class HelperFactory implements HelperFactoryConstructor {

    protected final DescriptorHandler descriptorHandler;

    public HelperFactory(DescriptorHandler descriptorHandler) {
        this.descriptorHandler = descriptorHandler;
    }

    protected DescriptorHandler getHandler() {
        return MiscUtil.cast(this.descriptorHandler);
    }

    @Override
    public ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, boolean prettyPrinting,
                                                                        DeployedMta deployedMta, String deployId, String namespace,
                                                                        UserMessageLogger stepLogger) {
        return new ApplicationCloudModelBuilder(deploymentDescriptor, prettyPrinting, deployedMta, deployId, namespace, stepLogger);
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor,
                                                                              ConfigurationEntryService configurationEntryService,
                                                                              CloudTarget cloudTarget,
                                                                              ApplicationConfiguration configuration, String namespace) {
        ParametersChainBuilder chainBuilder = new ParametersChainBuilder(deploymentDescriptor, null);
        com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser filterParser = new com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser(cloudTarget,
                                                                                                                                                             chainBuilder,
                                                                                                                                                             namespace);
        return new ConfigurationReferencesResolver(configurationEntryService, filterParser, cloudTarget, configuration);
    }

    @Override
    public ConfigurationReferencesResolver
           getConfigurationReferencesResolver(ConfigurationEntryService configurationEntryService, ConfigurationFilterParser filterParser,
                                              CloudTarget cloudTarget, ApplicationConfiguration configuration) {
        return new ConfigurationReferencesResolver(configurationEntryService, MiscUtil.cast(filterParser), cloudTarget, configuration);
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
    public DescriptorParametersCompatabilityValidator getDescriptorParametersCompatabilityValidator(DeploymentDescriptor descriptor,
                                                                                                    UserMessageLogger userMessageLogger) {
        return new DescriptorParametersCompatabilityValidator(descriptor, userMessageLogger);
    }

    @Override
    public PlatformMerger getPlatformMerger(Platform platform) {
        return new PlatformMerger(platform, getHandler());
    }

    @Override
    public ConfigurationSubscriptionFactory
           getConfigurationSubscriptionFactory(DeploymentDescriptor descriptor,
                                               Map<String, ResolvedConfigurationReference> resolvedReferences) {
        return new ConfigurationSubscriptionFactory(descriptor, resolvedReferences);
    }

    @Override
    public ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, String namespace) {
        return new ServicesCloudModelBuilder(deploymentDescriptor, namespace);
    }

    @Override
    public ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        return new ServiceKeysCloudModelBuilder(deploymentDescriptor);
    }

}
