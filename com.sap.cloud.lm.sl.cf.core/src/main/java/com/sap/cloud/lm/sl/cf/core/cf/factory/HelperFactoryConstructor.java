package com.sap.cloud.lm.sl.cf.core.cf.factory;

import java.util.List;

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
import com.sap.cloud.lm.sl.mta.mergers.PlatformMerger;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Platform;

public interface HelperFactoryConstructor {

    ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, boolean prettyPrinting,
                                                                 DeployedMta deployedMta, String deployId, UserMessageLogger stepLogger);

    ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor);

    ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor);

    ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor,
                                                                       ConfigurationEntryService configurationEntryService,
                                                                       CloudTarget cloudTarget, ApplicationConfiguration configuration);

    ConfigurationReferencesResolver getConfigurationReferencesResolver(ConfigurationEntryService configurationEntryService,
                                                                       ConfigurationFilterParser filterParser, CloudTarget cloudTarget,
                                                                       ApplicationConfiguration configuration);

    DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
                                                                   List<ParameterValidator> parameterValidators);

    DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
                                                                   List<ParameterValidator> parameterValidators, boolean doNotCorrect);

    PlatformMerger getPlatformMerger(Platform platform);

    ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory();

}
