package com.sap.cloud.lm.sl.cf.core.cf.factory;

import java.util.List;
import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.DomainsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ApplicationColorAppender;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.DeployTargetFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.OrgAndSpaceHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.v1_0.DescriptorParametersValidator;
import com.sap.cloud.lm.sl.mta.mergers.v1_0.PlatformMerger;
import com.sap.cloud.lm.sl.mta.mergers.v1_0.TargetMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public interface HelperFactoryConstructor {

    ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver, String deployId);

    ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver, String deployId, UserMessageLogger userMessageLogger);

    DomainsCloudModelBuilder getDomainsCloudModelBuilder(SystemParameters systemParameters, XsPlaceholderResolver xsPlaceholderResolver,
        DeploymentDescriptor deploymentDescriptor);

    ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, PropertiesAccessor propertiesAccessor,
        CloudModelConfiguration configuration);

    ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, PropertiesAccessor propertiesAccessor,
        CloudModelConfiguration configuration, UserMessageLogger userMessageLogger);

    DeployTargetFactory getDeployTargetFactory();

    DeployTargetDao<?, ?> getDeployTargetDao(com.sap.cloud.lm.sl.cf.core.dao.v1.DeployTargetDao dao1,
        com.sap.cloud.lm.sl.cf.core.dao.v2.DeployTargetDao dao2, com.sap.cloud.lm.sl.cf.core.dao.v3.DeployTargetDao dao3);

    ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor, Platform platform,
        Target target, BiFunction<String, String, String> spaceIdSupplier, ConfigurationEntryDao dao, CloudTarget cloudTarget, ApplicationConfiguration configuration);

    ConfigurationReferencesResolver getConfigurationReferencesResolver(ConfigurationEntryDao dao, ConfigurationFilterParser filterParser,
        CloudTarget cloudTarget, ApplicationConfiguration configuration);

    DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
        List<ParameterValidator> parameterValidators);

    DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
        List<ParameterValidator> parameterValidators, boolean doNotCorrect);

    ApplicationColorAppender getApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationType);

    ResourceTypeFinder getResourceTypeFinder(String resourceType);

    TargetMerger getTargetMerger(Target target);

    PlatformMerger getPlatformMerger(Platform platform);

    OrgAndSpaceHelper getOrgAndSpaceHelper(Target target, Platform platform);

    UserProvidedResourceResolver getUserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor,
        Target target, Platform platform);

    PropertiesAccessor getPropertiesAccessor();

    ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory();

}
