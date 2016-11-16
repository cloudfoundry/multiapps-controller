package com.sap.cloud.lm.sl.cf.core.cf.factory.v2_0;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.List;
import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.cf.v2_0.CloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.TargetPlatformDao;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ApplicationColorAppender;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.OrgAndSpaceHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.TargetPlatformFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.v2_0.DescriptorParametersValidator;
import com.sap.cloud.lm.sl.mta.builders.v2_0.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.mergers.v2_0.TargetPlatformMerger;
import com.sap.cloud.lm.sl.mta.mergers.v2_0.TargetPlatformTypeMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;

public class HelperFactory extends com.sap.cloud.lm.sl.cf.core.cf.factory.v1_0.HelperFactory {

    public HelperFactory(com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler descriptorHandler) {
        super(descriptorHandler);
    }

    protected DescriptorHandler getHandler() {
        return cast(super.getHandler());
    }

    @Override
    public CloudModelBuilder getCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, SystemParameters systemParameters,
        boolean portBasedRouting, boolean prettyPrinting, boolean useNamespaces, boolean useNamespacesForServices,
        boolean allowInvalidEnvNames, String deployId, XsPlaceholderResolver xsPlaceholderResolver) {
        return new CloudModelBuilder(cast(deploymentDescriptor), systemParameters, portBasedRouting, prettyPrinting, useNamespaces,
            useNamespacesForServices, allowInvalidEnvNames, deployId, xsPlaceholderResolver);
    }

    @Override
    public TargetPlatformFactory getTargetPlatformFactory() {
        return new TargetPlatformFactory();
    }

    @Override
    public TargetPlatformDao getTargetPlatformDao(com.sap.cloud.lm.sl.cf.core.dao.v1.TargetPlatformDao dao1,
        com.sap.cloud.lm.sl.cf.core.dao.v2.TargetPlatformDao dao2, com.sap.cloud.lm.sl.cf.core.dao.v3.TargetPlatformDao dao3) {
        return dao2;
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor,
        TargetPlatformType platformType, TargetPlatform platform, BiFunction<String, String, String> spaceIdSupplier,
        ConfigurationEntryDao dao) {
        ParametersChainBuilder chainBuilder = new ParametersChainBuilder(cast(deploymentDescriptor), cast(platform), cast(platformType));
        com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationFilterParser filterParser = new com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationFilterParser(
            cast(platformType), cast(platform), chainBuilder);
        return new ConfigurationReferencesResolver(dao, filterParser, spaceIdSupplier);
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(ConfigurationEntryDao dao,
        ConfigurationFilterParser filterParser) {
        return new ConfigurationReferencesResolver(dao, cast(filterParser), null);
    }

    @Override
    public DescriptorParametersValidator getDescriptorParametersValidator(
        com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor descriptor, List<ParameterValidator> parameterValidators) {
        return new DescriptorParametersValidator(cast(descriptor), parameterValidators);
    }

    @Override
    public DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
        List<ParameterValidator> parameterValidators, boolean doNotCorrect) {
        return new DescriptorParametersValidator(cast(descriptor), parameterValidators, doNotCorrect);
    }

    @Override
    public ApplicationColorAppender getApplicationColorAppender(ApplicationColor applicationType) {
        return new ApplicationColorAppender(applicationType);
    }

    @Override
    public com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ResourceTypeFinder getResourceTypeFinder(String resourceType) {
        return new com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ResourceTypeFinder(resourceType);

    }

    @Override
    public TargetPlatformMerger getTargetPlatformMerger(TargetPlatform platform) {
        return new TargetPlatformMerger(cast(platform), getHandler());
    }

    @Override
    public TargetPlatformTypeMerger getTargetPlatformTypeMerger(TargetPlatformType platformType) {
        return new TargetPlatformTypeMerger(cast(platformType), getHandler());
    }

    @Override
    public OrgAndSpaceHelper getOrgAndSpaceHelper(TargetPlatform platform, TargetPlatformType platformType) {
        return new OrgAndSpaceHelper(cast(platform), cast(platformType));
    }

    @Override
    public UserProvidedResourceResolver getUserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor,
        TargetPlatform platform, TargetPlatformType platformType) {
        return new UserProvidedResourceResolver(resourceHelper, cast(descriptor), cast(platform), cast(platformType));
    }

    @Override
    public PropertiesAccessor getPropertiesAccessor() {
        return new PropertiesAccessor();
    }

    @Override
    public ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory() {
        return new ConfigurationSubscriptionFactory();
    }

}
