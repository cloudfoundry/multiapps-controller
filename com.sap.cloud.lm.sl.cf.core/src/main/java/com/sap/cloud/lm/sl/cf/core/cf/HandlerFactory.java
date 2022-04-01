package com.sap.cloud.lm.sl.cf.core.cf;

import java.util.List;
import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.cf.factory.HelperFactoryConstructor;
import com.sap.cloud.lm.sl.cf.core.cf.factory.v2.HelperFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ApplicationColorAppender;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.v2.DescriptorParametersValidator;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.mergers.v2.PlatformMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;

public class HandlerFactory extends com.sap.cloud.lm.sl.mta.handlers.HandlerFactory implements HelperFactoryConstructor {

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
                                                                              BiFunction<String, String, String> spaceIdSupplier,
                                                                              ConfigurationEntryDao dao, CloudTarget cloudTarget,
                                                                              ApplicationConfiguration configuration) {
        return getHelperDelegate().getConfigurationReferencesResolver(deploymentDescriptor, platform, spaceIdSupplier, dao, cloudTarget,
                                                                      configuration);
    }

    @Override
    public ConfigurationReferencesResolver
           getConfigurationReferencesResolver(ConfigurationEntryDao dao, ConfigurationFilterParser filterParser, CloudTarget cloudTarget,
                                              ApplicationConfiguration configuration) {
        return getHelperDelegate().getConfigurationReferencesResolver(dao, filterParser, cloudTarget, configuration);
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
                                                                          CloudModelConfiguration configuration, DeployedMta deployedMta,
                                                                          SystemParameters systemParameters,
                                                                          XsPlaceholderResolver xsPlaceholderResolver, String deployId) {
        return getHelperDelegate().getApplicationsCloudModelBuilder(deploymentDescriptor, configuration, deployedMta, systemParameters,
                                                                    xsPlaceholderResolver, deployId);
    }

    @Override
    public ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
                                                                  PropertiesAccessor propertiesAccessor,
                                                                  CloudModelConfiguration configuration) {
        return getHelperDelegate().getServicesCloudModelBuilder(deploymentDescriptor, propertiesAccessor, configuration);
    }

    @Override
    public ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
                                                                        PropertiesAccessor propertiesAccessor) {
        return getHelperDelegate().getServiceKeysCloudModelBuilder(deploymentDescriptor, propertiesAccessor);
    }

}
