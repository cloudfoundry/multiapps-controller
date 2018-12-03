package com.sap.cloud.lm.sl.cf.core.cf.factory.v3;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.cf.v1.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v1.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v3.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.helpers.v1.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v3.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v3.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v3.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1.Platform;

public class HelperFactory extends com.sap.cloud.lm.sl.cf.core.cf.factory.v2.HelperFactory {

    public HelperFactory(com.sap.cloud.lm.sl.mta.handlers.v1.DescriptorHandler descriptorHandler) {
        super(descriptorHandler);
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor, Platform platform,
        BiFunction<String, String, String> spaceIdSupplier, ConfigurationEntryDao dao, CloudTarget cloudTarget,
        ApplicationConfiguration configuration) {
        ParametersChainBuilder v2ParameterChainBuilder = new ParametersChainBuilder(cast(deploymentDescriptor), cast(platform));
        ConfigurationFilterParser v2FilterParser = new ConfigurationFilterParser(cloudTarget, v2ParameterChainBuilder);
        return new ConfigurationReferencesResolver(dao, v2FilterParser, spaceIdSupplier, cloudTarget, configuration);
    }

    @Override
    public UserProvidedResourceResolver getUserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor,
        Platform platform) {
        return new UserProvidedResourceResolver(resourceHelper, cast(descriptor), cast(platform));
    }

    @Override
    public ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver, String deployId) {
        return new ApplicationsCloudModelBuilder((com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor) deploymentDescriptor,
            configuration, deployedMta, systemParameters, xsPlaceholderResolver, deployId);
    }

    @Override
    public ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver, String deployId, UserMessageLogger userMessageLogger) {
        return new ApplicationsCloudModelBuilder((com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor) deploymentDescriptor,
            configuration, deployedMta, systemParameters, xsPlaceholderResolver, deployId, userMessageLogger);
    }

    @Override
    public ConfigurationSubscriptionFactory getConfigurationSubscriptionFactory() {
        return new ConfigurationSubscriptionFactory();
    }

    @Override
    public ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        PropertiesAccessor propertiesAccessor, CloudModelConfiguration configuration) {
        return new com.sap.cloud.lm.sl.cf.core.cf.v3.ServicesCloudModelBuilder(deploymentDescriptor, propertiesAccessor, configuration);
    }

    @Override
    public ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        PropertiesAccessor propertiesAccessor, CloudModelConfiguration configuration, UserMessageLogger userMessageLogger) {
        return new com.sap.cloud.lm.sl.cf.core.cf.v3.ServicesCloudModelBuilder(deploymentDescriptor, propertiesAccessor, configuration,
            userMessageLogger);
    }

    @Override
    public ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        PropertiesAccessor propertiesAccessor) {
        return new com.sap.cloud.lm.sl.cf.core.cf.v3.ServiceKeysCloudModelBuilder(deploymentDescriptor, propertiesAccessor);
    }

}
