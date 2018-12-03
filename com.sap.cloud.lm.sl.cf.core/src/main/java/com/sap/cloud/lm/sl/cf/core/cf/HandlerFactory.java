package com.sap.cloud.lm.sl.cf.core.cf;

import java.util.List;
import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.cf.factory.HelperFactoryConstructor;
import com.sap.cloud.lm.sl.cf.core.cf.v1.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v1.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.ApplicationColorAppender;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.v1.DescriptorParametersValidator;
import com.sap.cloud.lm.sl.mta.handlers.v1.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.mergers.v1.PlatformMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1.Platform;

public class HandlerFactory extends com.sap.cloud.lm.sl.mta.handlers.HandlerFactory implements HelperFactoryConstructor {

    private com.sap.cloud.lm.sl.cf.core.cf.factory.v1.HelperFactory helperDelegate;

    public HandlerFactory(int majorVersion) {
        super(majorVersion);
    }

    public com.sap.cloud.lm.sl.cf.core.cf.factory.v1.HelperFactory getHelperDelegate() {
        if (helperDelegate == null) {
            super.initDelegates();
        }
        return helperDelegate;
    }

    @Override
    protected void initV1Delegates() {
        super.initV1Delegates();
        helperDelegate = new com.sap.cloud.lm.sl.cf.core.cf.factory.v1.HelperFactory(getDescriptorHandler());
    }

    @Override
    protected void initV2Delegates() {
        super.initV2Delegates();
        helperDelegate = new com.sap.cloud.lm.sl.cf.core.cf.factory.v2.HelperFactory(getDescriptorHandler());
    }

    @Override
    public DescriptorHandler getDescriptorHandler() {
        return getHandlerDelegate().getDescriptorHandler();
    }

    @Override
    protected void initV3Delegates() {
        super.initV3Delegates();
        helperDelegate = new com.sap.cloud.lm.sl.cf.core.cf.factory.v3.HelperFactory(getDescriptorHandler());
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor, Platform platform,
        BiFunction<String, String, String> spaceIdSupplier, ConfigurationEntryDao dao, CloudTarget cloudTarget,
        ApplicationConfiguration configuration) {
        return getHelperDelegate().getConfigurationReferencesResolver(deploymentDescriptor, platform, spaceIdSupplier, dao, cloudTarget,
            configuration);
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(ConfigurationEntryDao dao,
        ConfigurationFilterParser filterParser, CloudTarget cloudTarget, ApplicationConfiguration configuration) {
        return getHelperDelegate().getConfigurationReferencesResolver(dao, filterParser, cloudTarget, configuration);
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
    public ApplicationColorAppender getApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationType) {
        return getHelperDelegate().getApplicationColorAppender(deployedMtaColor, applicationType);
    }

    @Override
    public ResourceTypeFinder getResourceTypeFinder(String resourceType) {
        return getHelperDelegate().getResourceTypeFinder(resourceType);
    }

    @Override
    public PlatformMerger getPlatformMerger(Platform platform) {
        return getHelperDelegate().getPlatformMerger(platform);
    }

    @Override
    public UserProvidedResourceResolver getUserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor,
        Platform platform) {
        return getHelperDelegate().getUserProvidedResourceResolver(resourceHelper, descriptor, platform);
    }

    @Override
    public PropertiesAccessor getPropertiesAccessor() {
        return getHelperDelegate().getPropertiesAccessor();
    }

    @Override
    public ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory() {
        return getHelperDelegate().getConfigurationSubscriptionFactory();
    }

    @Override
    public ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver, String deployId) {
        return getHelperDelegate().getApplicationsCloudModelBuilder(deploymentDescriptor, configuration, deployedMta, systemParameters,
            xsPlaceholderResolver, deployId);
    }

    @Override
    public ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver, String deployId, UserMessageLogger userMessageLogger) {
        return getHelperDelegate().getApplicationsCloudModelBuilder(deploymentDescriptor, configuration, deployedMta, systemParameters,
            xsPlaceholderResolver, deployId, userMessageLogger);
    }

    @Override
    public ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        PropertiesAccessor propertiesAccessor, CloudModelConfiguration configuration) {
        return getHelperDelegate().getServicesCloudModelBuilder(deploymentDescriptor, propertiesAccessor, configuration);
    }

    @Override
    public ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        PropertiesAccessor propertiesAccessor, CloudModelConfiguration configuration, UserMessageLogger userMessageLogger) {
        return getHelperDelegate().getServicesCloudModelBuilder(deploymentDescriptor, propertiesAccessor, configuration, userMessageLogger);
    }

    @Override
    public ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        PropertiesAccessor propertiesAccessor) {
        return getHelperDelegate().getServiceKeysCloudModelBuilder(deploymentDescriptor, propertiesAccessor);
    }

}
