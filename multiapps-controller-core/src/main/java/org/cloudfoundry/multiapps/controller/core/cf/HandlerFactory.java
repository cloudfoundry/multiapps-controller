package org.cloudfoundry.multiapps.controller.core.cf;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.cf.factory.HelperFactoryConstructor;
import org.cloudfoundry.multiapps.controller.core.cf.factory.v2.HelperFactory;
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
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandler;
import org.cloudfoundry.multiapps.mta.mergers.PlatformMerger;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;

public class HandlerFactory extends org.cloudfoundry.multiapps.mta.handlers.HandlerFactory implements HelperFactoryConstructor {

    private HelperFactory helperDelegate;

    public HandlerFactory(int majorVersion) {
        super(majorVersion);
    }

    public HelperFactory getHelperDelegate() {
        if (helperDelegate == null) {
            super.initDelegates();
        }
        return helperDelegate;
    }

    @Override
    protected void initV2Delegates() {
        super.initV2Delegates();
        helperDelegate = new org.cloudfoundry.multiapps.controller.core.cf.factory.v2.HelperFactory(getDescriptorHandler());
    }

    @Override
    public DescriptorHandler getDescriptorHandler() {
        return getHandlerDelegate().getDescriptorHandler();
    }

    @Override
    protected void initV3Delegates() {
        super.initV3Delegates();
        helperDelegate = new org.cloudfoundry.multiapps.controller.core.cf.factory.v3.HelperFactory(getDescriptorHandler());
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor,
                                                                              ConfigurationEntryService configurationEntryService,
                                                                              CloudTarget cloudTarget,
                                                                              ApplicationConfiguration configuration, String namespace) {
        return getHelperDelegate().getConfigurationReferencesResolver(deploymentDescriptor, configurationEntryService, cloudTarget,
                                                                      configuration, namespace);
    }

    @Override
    public ConfigurationReferencesResolver
           getConfigurationReferencesResolver(ConfigurationEntryService configurationEntryService, ConfigurationFilterParser filterParser,
                                              CloudTarget cloudTarget, ApplicationConfiguration configuration) {
        return getHelperDelegate().getConfigurationReferencesResolver(configurationEntryService, filterParser, cloudTarget, configuration);
    }

    @Override
    public DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
                                                                          List<ParameterValidator> parameterValidators) {
        return getHelperDelegate().getDescriptorParametersValidator(descriptor, parameterValidators);
    }

    @Override
    public DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
                                                                          List<ParameterValidator> parameterValidators,
                                                                          boolean doNotCorrect) {
        return getHelperDelegate().getDescriptorParametersValidator(descriptor, parameterValidators);
    }

    @Override
    public DescriptorParametersCompatabilityValidator getDescriptorParametersCompatabilityValidator(DeploymentDescriptor descriptor,
                                                                                                    UserMessageLogger userMessageLogger) {
        return getHelperDelegate().getDescriptorParametersCompatabilityValidator(descriptor, userMessageLogger);
    }

    @Override
    public PlatformMerger getPlatformMerger(Platform platform) {
        return getHelperDelegate().getPlatformMerger(platform);
    }

    @Override
    public ConfigurationSubscriptionFactory
           getConfigurationSubscriptionFactory(DeploymentDescriptor descriptor,
                                               Map<String, ResolvedConfigurationReference> resolvedReferences) {
        return getHelperDelegate().getConfigurationSubscriptionFactory(descriptor, resolvedReferences);
    }

    @Override
    public ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, boolean prettyPrinting,
                                                                        DeployedMta deployedMta, String deployId, String namespace,
                                                                        UserMessageLogger stepLogger) {
        return getHelperDelegate().getApplicationCloudModelBuilder(deploymentDescriptor, prettyPrinting, deployedMta, deployId, namespace,
                                                                   stepLogger);
    }

    @Override
    public ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, String namespace) {
        return getHelperDelegate().getServicesCloudModelBuilder(deploymentDescriptor, namespace);
    }

    @Override
    public ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        return getHelperDelegate().getServiceKeysCloudModelBuilder(deploymentDescriptor);
    }

}
