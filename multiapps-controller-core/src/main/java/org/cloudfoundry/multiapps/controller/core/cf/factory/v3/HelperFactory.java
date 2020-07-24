package org.cloudfoundry.multiapps.controller.core.cf.factory.v3;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.cf.v2.ServiceKeysCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ServicesCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.v3.ApplicationCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationFilterParser;
import org.cloudfoundry.multiapps.controller.core.helpers.v3.ConfigurationReferencesResolver;
import org.cloudfoundry.multiapps.controller.core.helpers.v3.ConfigurationSubscriptionFactory;
import org.cloudfoundry.multiapps.controller.core.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ResolvedConfigurationReference;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.builders.v2.ParametersChainBuilder;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandler;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

public class HelperFactory extends org.cloudfoundry.multiapps.controller.core.cf.factory.v2.HelperFactory {
    public HelperFactory(DescriptorHandler descriptorHandler) {
        super(descriptorHandler);
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
    public ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, boolean prettyPrinting,
                                                                        DeployedMta deployedMta, String deployId, String namespace,
                                                                        UserMessageLogger stepLogger) {
        return new ApplicationCloudModelBuilder(deploymentDescriptor, prettyPrinting, deployedMta, deployId, namespace, stepLogger);
    }

    @Override
    public ConfigurationSubscriptionFactory
           getConfigurationSubscriptionFactory(DeploymentDescriptor descriptor,
                                               Map<String, ResolvedConfigurationReference> resolvedReferences) {
        return new ConfigurationSubscriptionFactory(descriptor, resolvedReferences);
    }

    @Override
    public ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, String namespace) {
        return new org.cloudfoundry.multiapps.controller.core.cf.v3.ServicesCloudModelBuilder(deploymentDescriptor, namespace);
    }

    @Override
    public ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        return new org.cloudfoundry.multiapps.controller.core.cf.v3.ServiceKeysCloudModelBuilder(deploymentDescriptor);
    }

}
