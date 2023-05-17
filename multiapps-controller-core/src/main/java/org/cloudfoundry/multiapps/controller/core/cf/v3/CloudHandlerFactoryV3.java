package org.cloudfoundry.multiapps.controller.core.cf.v3;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.cf.detect.AppSuffixDeterminer;
import org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationFilterParser;
import org.cloudfoundry.multiapps.controller.core.helpers.v3.ConfigurationReferencesResolver;
import org.cloudfoundry.multiapps.controller.core.helpers.v3.ConfigurationSubscriptionFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.v3.DynamicResolvableParametersFactory;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ResolvedConfigurationReference;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.ParameterValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.v2.DescriptorParametersCompatabilityValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.v2.DescriptorParametersValidator;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.mta.builders.v2.ParametersChainBuilder;
import org.cloudfoundry.multiapps.mta.handlers.v3.HandlerFactoryV3;
import org.cloudfoundry.multiapps.mta.mergers.PlatformMerger;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

public class CloudHandlerFactoryV3 extends HandlerFactoryV3 implements CloudHandlerFactory {

    @Override
    public ApplicationCloudModelBuilder
           getApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, boolean prettyPrinting, DeployedMta deployedMta,
                                           String deployId, String namespace, UserMessageLogger stepLogger,
                                           AppSuffixDeterminer appSuffixDeterminer, CloudControllerClient client) {
        return new ApplicationCloudModelBuilder.Builder().deploymentDescriptor(deploymentDescriptor)
                                                         .prettyPrinting(prettyPrinting)
                                                         .deployedMta(deployedMta)
                                                         .deployId(deployId)
                                                         .namespace(namespace)
                                                         .userMessageLogger(stepLogger)
                                                         .appSuffixDeterminer(appSuffixDeterminer)
                                                         .client(client)
                                                         .build();
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor,
                                                                              ConfigurationEntryService configurationEntryService,
                                                                              CloudTarget cloudTarget,
                                                                              ApplicationConfiguration configuration, String namespace) {
        ParametersChainBuilder v2ParameterChainBuilder = new ParametersChainBuilder(deploymentDescriptor, null);
        ConfigurationFilterParser v2FilterParser = new ConfigurationFilterParser(cloudTarget, v2ParameterChainBuilder, namespace);
        return new ConfigurationReferencesResolver(configurationEntryService, v2FilterParser, cloudTarget, configuration);
    }

    @Override
    public ConfigurationReferencesResolver
           getConfigurationReferencesResolver(ConfigurationEntryService configurationEntryService, ConfigurationFilterParser filterParser,
                                              CloudTarget cloudTarget, ApplicationConfiguration configuration) {
        return new ConfigurationReferencesResolver(configurationEntryService, filterParser, cloudTarget, configuration);
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
        return new PlatformMerger(platform, getDescriptorHandler());
    }

    @Override
    public ConfigurationSubscriptionFactory
           getConfigurationSubscriptionFactory(DeploymentDescriptor descriptor,
                                               Map<String, ResolvedConfigurationReference> resolvedReferences,
                                               Set<String> dynamicResolvableParameters) {
        return new ConfigurationSubscriptionFactory(descriptor, resolvedReferences, dynamicResolvableParameters);
    }

    @Override
    public DynamicResolvableParametersFactory getDynamicResolvableParameterFactory(DeploymentDescriptor descriptior) {
        return new DynamicResolvableParametersFactory(descriptior);
    }

    @Override
    public ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, String namespace) {
        return new ServicesCloudModelBuilder(deploymentDescriptor, namespace);
    }

    @Override
    public ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, String namespace,
                                                                        String spaceGuid) {
        return new ServiceKeysCloudModelBuilder(deploymentDescriptor, namespace, spaceGuid);
    }

}
