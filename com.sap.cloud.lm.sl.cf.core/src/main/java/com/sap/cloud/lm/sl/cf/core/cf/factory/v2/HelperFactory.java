package com.sap.cloud.lm.sl.cf.core.cf.factory.v2;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.List;
import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.cf.v1.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ApplicationColorAppender;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.v2.DescriptorParametersValidator;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.mergers.v2.PlatformMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1.Platform;

public class HelperFactory extends com.sap.cloud.lm.sl.cf.core.cf.factory.v1.HelperFactory {

    public HelperFactory(com.sap.cloud.lm.sl.mta.handlers.v1.DescriptorHandler descriptorHandler) {
        super(descriptorHandler);
    }

    @Override
    protected DescriptorHandler getHandler() {
        return cast(super.getHandler());
    }

    @Override
    public ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver, String deployId) {
        return new ApplicationsCloudModelBuilder((com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor) deploymentDescriptor,
            configuration, deployedMta, systemParameters, xsPlaceholderResolver, deployId);
    }

    @Override
    public ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver, String deployId, UserMessageLogger userMessageLogger) {
        return new ApplicationsCloudModelBuilder((com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor) deploymentDescriptor,
            configuration, deployedMta, systemParameters, xsPlaceholderResolver, deployId, userMessageLogger);
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor, Platform platform,
        BiFunction<String, String, String> spaceIdSupplier, ConfigurationEntryDao dao, CloudTarget cloudTarget,
        ApplicationConfiguration configuration) {
        ParametersChainBuilder chainBuilder = new ParametersChainBuilder(cast(deploymentDescriptor), cast(platform));
        com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser filterParser = new com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser(
            cloudTarget, chainBuilder);
        return new ConfigurationReferencesResolver(dao, filterParser, spaceIdSupplier, cloudTarget, configuration);
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(ConfigurationEntryDao dao,
        ConfigurationFilterParser filterParser, CloudTarget cloudTarget, ApplicationConfiguration configuration) {
        return new ConfigurationReferencesResolver(dao, cast(filterParser), null, cloudTarget, configuration);
    }

    @Override
    public DescriptorParametersValidator getDescriptorParametersValidator(com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor descriptor,
        List<ParameterValidator> parameterValidators) {
        return new DescriptorParametersValidator(cast(descriptor), parameterValidators);
    }

    @Override
    public DescriptorParametersValidator getDescriptorParametersValidator(DeploymentDescriptor descriptor,
        List<ParameterValidator> parameterValidators, boolean doNotCorrect) {
        return new DescriptorParametersValidator(cast(descriptor), parameterValidators, doNotCorrect);
    }

    @Override
    public ApplicationColorAppender getApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationType) {
        return new ApplicationColorAppender(deployedMtaColor, applicationType);
    }

    @Override
    public com.sap.cloud.lm.sl.cf.core.helpers.v2.ResourceTypeFinder getResourceTypeFinder(String resourceType) {
        return new com.sap.cloud.lm.sl.cf.core.helpers.v2.ResourceTypeFinder(resourceType);

    }

    @Override
    public PlatformMerger getPlatformMerger(Platform platform) {
        return new PlatformMerger(cast(platform), getHandler());
    }

    @Override
    public UserProvidedResourceResolver getUserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor,
        Platform platform) {
        return new UserProvidedResourceResolver(resourceHelper, cast(descriptor), cast(platform));
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
