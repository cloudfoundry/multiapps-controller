package com.sap.cloud.lm.sl.cf.core.cf;

import java.util.List;
import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.cf.factory.HelperFactoryConstructor;
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
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.mergers.v1_0.TargetPlatformMerger;
import com.sap.cloud.lm.sl.mta.mergers.v1_0.TargetPlatformTypeMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;

public class HandlerFactory extends com.sap.cloud.lm.sl.mta.handlers.HandlerFactory implements HelperFactoryConstructor {

    private com.sap.cloud.lm.sl.cf.core.cf.factory.v1_0.HelperFactory helperDelegate;

    public HandlerFactory(int majorVersion) {
        super(majorVersion);
    }

    public HandlerFactory(int majorVersion, int minorVersion) {
        super(majorVersion, minorVersion);
    }

    public com.sap.cloud.lm.sl.cf.core.cf.factory.v1_0.HelperFactory getHelperDelegate() {
        if (helperDelegate == null) {
            super.initDelegates();
        }
        return helperDelegate;
    }

    @Override
    protected void initV1Delegates() {
        super.initV1Delegates();
        helperDelegate = new com.sap.cloud.lm.sl.cf.core.cf.factory.v1_0.HelperFactory(getDescriptorHandler());
    }

    @Override
    protected void initV2Delegates() {
        super.initV2Delegates();
        helperDelegate = new com.sap.cloud.lm.sl.cf.core.cf.factory.v2_0.HelperFactory(getDescriptorHandler());
    }

    @Override
    public DescriptorHandler getDescriptorHandler() {
        return getHandlerDelegate().getDescriptorHandler();
    }

    @Override
    protected void initV3_0Delegates() {
        super.initV3_0Delegates();
        helperDelegate = new com.sap.cloud.lm.sl.cf.core.cf.factory.v3_0.HelperFactory(getDescriptorHandler());
    }

    @Override
    protected void initV3_1Delegates() {
        super.initV3_1Delegates();
        helperDelegate = new com.sap.cloud.lm.sl.cf.core.cf.factory.v3_1.HelperFactory(getDescriptorHandler());
    }

    @Override
    public CloudModelBuilder getCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, SystemParameters systemParameters,
        boolean portBasedRouting, boolean prettyPrinting, boolean useNamespaces, boolean useNamespacesForServices,
        boolean allowInvalidEnvNames, String deployId, XsPlaceholderResolver xsPlaceholderResolver) {
        return getHelperDelegate().getCloudModelBuilder(deploymentDescriptor, systemParameters, portBasedRouting, prettyPrinting,
            useNamespaces, useNamespacesForServices, allowInvalidEnvNames, deployId, xsPlaceholderResolver);
    }

    @Override
    public TargetPlatformFactory getTargetPlatformFactory() {
        return getHelperDelegate().getTargetPlatformFactory();
    }

    @Override
    public TargetPlatformDao getTargetPlatformDao(com.sap.cloud.lm.sl.cf.core.dao.v1.TargetPlatformDao dao1,
        com.sap.cloud.lm.sl.cf.core.dao.v2.TargetPlatformDao dao2, com.sap.cloud.lm.sl.cf.core.dao.v3.TargetPlatformDao dao3) {
        return getHelperDelegate().getTargetPlatformDao(dao1, dao2, dao3);
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor,
        TargetPlatformType platformType, TargetPlatform platform, BiFunction<String, String, String> spaceIdSupplier,
        ConfigurationEntryDao dao) {
        return getHelperDelegate().getConfigurationReferencesResolver(deploymentDescriptor, platformType, platform, spaceIdSupplier, dao);
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(ConfigurationEntryDao dao,
        ConfigurationFilterParser filterParser) {
        return getHelperDelegate().getConfigurationReferencesResolver(dao, filterParser);
    }

    @Override
    public DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
        List<ParameterValidator> parameterValidators) {
        return getHelperDelegate().getDescriptorParametersValidator(descriptor, parameterValidators);
    }

    @Override
    public DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
        List<ParameterValidator> parameterValidators, boolean doNotCorrect) {
        return getHelperDelegate().getDescriptorParametersValidator(descriptor, parameterValidators);
    }

    @Override
    public ApplicationColorAppender getApplicationColorAppender(ApplicationColor applicationType) {
        return getHelperDelegate().getApplicationColorAppender(applicationType);
    }

    @Override
    public ResourceTypeFinder getResourceTypeFinder(String resourceType) {
        return getHelperDelegate().getResourceTypeFinder(resourceType);
    }

    @Override
    public TargetPlatformMerger getTargetPlatformMerger(TargetPlatform platform) {
        return getHelperDelegate().getTargetPlatformMerger(platform);
    }

    @Override
    public TargetPlatformTypeMerger getTargetPlatformTypeMerger(TargetPlatformType platformType) {
        return getHelperDelegate().getTargetPlatformTypeMerger(platformType);
    }

    @Override
    public OrgAndSpaceHelper getOrgAndSpaceHelper(TargetPlatform platform, TargetPlatformType platformType) {
        return getHelperDelegate().getOrgAndSpaceHelper(platform, platformType);
    }

    @Override
    public UserProvidedResourceResolver getUserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor,
        TargetPlatform platform, TargetPlatformType platformType) {
        return getHelperDelegate().getUserProvidedResourceResolver(resourceHelper, descriptor, platform, platformType);
    }

    @Override
    public PropertiesAccessor getPropertiesAccessor() {
        return getHelperDelegate().getPropertiesAccessor();
    }

    @Override
    public ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory() {
        return getHelperDelegate().getConfigurationSubscriptionFactory();
    }

}
