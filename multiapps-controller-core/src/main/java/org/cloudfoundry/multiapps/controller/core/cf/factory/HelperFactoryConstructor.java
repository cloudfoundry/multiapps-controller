package org.cloudfoundry.multiapps.controller.core.cf.factory;

import java.util.List;
import java.util.Map;

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
import org.cloudfoundry.multiapps.mta.mergers.PlatformMerger;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;

public interface HelperFactoryConstructor {

    ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, boolean prettyPrinting,
                                                                 DeployedMta deployedMta, String deployId, String namespace,
                                                                 UserMessageLogger stepLogger);

    ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, String namespace);

    ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor);

    ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor,
                                                                       ConfigurationEntryService configurationEntryService,
                                                                       CloudTarget cloudTarget, ApplicationConfiguration configuration,
                                                                       String namespace);

    ConfigurationReferencesResolver getConfigurationReferencesResolver(ConfigurationEntryService configurationEntryService,
                                                                       ConfigurationFilterParser filterParser, CloudTarget cloudTarget,
                                                                       ApplicationConfiguration configuration);

    DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
                                                                   List<ParameterValidator> parameterValidators);

    DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
                                                                   List<ParameterValidator> parameterValidators, boolean doNotCorrect);

    DescriptorParametersCompatabilityValidator getDescriptorParametersCompatabilityValidator(DeploymentDescriptor descriptor,
                                                                                             UserMessageLogger userMessageLogger);

    PlatformMerger getPlatformMerger(Platform platform);

    ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory(DeploymentDescriptor descriptor,
                                                                         Map<String, ResolvedConfigurationReference> resolvedReferences);

}
