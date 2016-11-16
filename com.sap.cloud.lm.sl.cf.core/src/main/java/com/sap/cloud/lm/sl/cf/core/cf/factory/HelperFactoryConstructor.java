package com.sap.cloud.lm.sl.cf.core.cf.factory;

import java.util.List;
import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.TargetPlatformDao;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ApplicationColorAppender;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.OrgAndSpaceHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.TargetPlatformFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.v1_0.DescriptorParametersValidator;
import com.sap.cloud.lm.sl.mta.mergers.v1_0.TargetPlatformMerger;
import com.sap.cloud.lm.sl.mta.mergers.v1_0.TargetPlatformTypeMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;

public interface HelperFactoryConstructor {

    CloudModelBuilder getCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, SystemParameters systemParameters,
        boolean portBasedRouting, boolean prettyPrinting, boolean useNamespaces, boolean useNamespacesForServices,
        boolean allowInvalidEnvNames, String deployId, XsPlaceholderResolver xsPlaceholderResolver);

    TargetPlatformFactory getTargetPlatformFactory();

    TargetPlatformDao getTargetPlatformDao(com.sap.cloud.lm.sl.cf.core.dao.v1.TargetPlatformDao dao1,
        com.sap.cloud.lm.sl.cf.core.dao.v2.TargetPlatformDao dao2, com.sap.cloud.lm.sl.cf.core.dao.v3.TargetPlatformDao dao3);

    ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor,
        TargetPlatformType platformType, TargetPlatform platform, BiFunction<String, String, String> spaceIdSupplier,
        ConfigurationEntryDao dao);

    ConfigurationReferencesResolver getConfigurationReferencesResolver(ConfigurationEntryDao dao, ConfigurationFilterParser filterParser);

    DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
        List<ParameterValidator> parameterValidators);

    DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
        List<ParameterValidator> parameterValidators, boolean doNotCorrect);

    ApplicationColorAppender getApplicationColorAppender(ApplicationColor applicationType);

    ResourceTypeFinder getResourceTypeFinder(String resourceType);

    TargetPlatformMerger getTargetPlatformMerger(TargetPlatform platform);

    TargetPlatformTypeMerger getTargetPlatformTypeMerger(TargetPlatformType platformType);

    OrgAndSpaceHelper getOrgAndSpaceHelper(TargetPlatform platform, TargetPlatformType platformType);

    UserProvidedResourceResolver getUserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor,
        TargetPlatform platform, TargetPlatformType platformType);

    PropertiesAccessor getPropertiesAccessor();

    ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory();

}
