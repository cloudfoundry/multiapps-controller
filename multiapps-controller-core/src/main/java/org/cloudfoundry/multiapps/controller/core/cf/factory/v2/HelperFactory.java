package org.cloudfoundry.multiapps.controller.core.cf.factory.v2;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.core.cf.factory.HelperFactoryConstructor;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ApplicationCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ServiceKeysCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ServicesCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationFilterParser;
import org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationReferencesResolver;
import org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationSubscriptionFactory;
import org.cloudfoundry.multiapps.controller.core.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ResolvedConfigurationReference;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.ParameterValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.v2.DescriptorParametersCompatabilityValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.v2.DescriptorParametersValidator;
import org.cloudfoundry.multiapps.mta.builders.v2.ParametersChainBuilder;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandler;
import org.cloudfoundry.multiapps.mta.mergers.PlatformMerger;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;

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
        org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationFilterParser filterParser = new org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationFilterParser(cloudTarget,
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
