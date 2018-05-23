package com.sap.cloud.lm.sl.cf.core.cf.factory.v2_0;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.List;
import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v2_0.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2_0.DomainsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ApplicationColorAppender;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationSubscriptionFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.DeployTargetFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.OrgAndSpaceHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.v2_0.DescriptorParametersValidator;
import com.sap.cloud.lm.sl.mta.builders.v2_0.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.mergers.v2_0.PlatformMerger;
import com.sap.cloud.lm.sl.mta.mergers.v2_0.TargetMerger;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public class HelperFactory extends com.sap.cloud.lm.sl.cf.core.cf.factory.v1_0.HelperFactory {

    public HelperFactory(com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler descriptorHandler) {
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
        return new ApplicationsCloudModelBuilder((com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor) deploymentDescriptor,
            configuration, deployedMta, systemParameters, xsPlaceholderResolver, deployId);
    }

    @Override
    public ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver, String deployId, UserMessageLogger userMessageLogger) {
        return new ApplicationsCloudModelBuilder((com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor) deploymentDescriptor,
            configuration, deployedMta, systemParameters, xsPlaceholderResolver, deployId, userMessageLogger);
    }

    @Override
    public DomainsCloudModelBuilder getDomainsCloudModelBuilder(SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver, DeploymentDescriptor deploymentDescriptor) {
        return new DomainsCloudModelBuilder(systemParameters, xsPlaceholderResolver,
            (com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor) deploymentDescriptor);
    }

    @Override
    public DeployTargetFactory getDeployTargetFactory() {
        return new DeployTargetFactory();
    }

    @Override
    public DeployTargetDao<?, ?> getDeployTargetDao(com.sap.cloud.lm.sl.cf.core.dao.v1.DeployTargetDao dao1,
        com.sap.cloud.lm.sl.cf.core.dao.v2.DeployTargetDao dao2, com.sap.cloud.lm.sl.cf.core.dao.v3.DeployTargetDao dao3) {
        return dao2;
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(DeploymentDescriptor deploymentDescriptor, Platform platform,
        Target target, BiFunction<String, String, String> spaceIdSupplier, ConfigurationEntryDao dao, CloudTarget cloudTarget,
        ApplicationConfiguration configuration) {
        ParametersChainBuilder chainBuilder = new ParametersChainBuilder(cast(deploymentDescriptor), cast(target), cast(platform));
        com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationFilterParser filterParser = new com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationFilterParser(
            cast(platform), cast(target), chainBuilder);
        return new ConfigurationReferencesResolver(dao, filterParser, spaceIdSupplier, cloudTarget, configuration);
    }

    @Override
    public ConfigurationReferencesResolver getConfigurationReferencesResolver(ConfigurationEntryDao dao,
        ConfigurationFilterParser filterParser, CloudTarget cloudTarget, ApplicationConfiguration configuration) {
        return new ConfigurationReferencesResolver(dao, cast(filterParser), null, cloudTarget, configuration);
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
    public ApplicationColorAppender getApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationType) {
        return new ApplicationColorAppender(deployedMtaColor, applicationType);
    }

    @Override
    public com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ResourceTypeFinder getResourceTypeFinder(String resourceType) {
        return new com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ResourceTypeFinder(resourceType);

    }

    @Override
    public TargetMerger getTargetMerger(Target target) {
        return new TargetMerger(cast(target), getHandler());
    }

    @Override
    public PlatformMerger getPlatformMerger(Platform platform) {
        return new PlatformMerger(cast(platform), getHandler());
    }

    @Override
    public OrgAndSpaceHelper getOrgAndSpaceHelper(Target target, Platform platform) {
        return new OrgAndSpaceHelper(cast(target), cast(platform));
    }

    @Override
    public UserProvidedResourceResolver getUserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor,
        Target target, Platform platform) {
        return new UserProvidedResourceResolver(resourceHelper, cast(descriptor), cast(target), cast(platform));
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
