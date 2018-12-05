package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.clients.SpaceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ResourceType;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorPropertiesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolverInvoker;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;

public class ProcessDescriptorStep extends SyncFlowableStep {

    protected SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    private ConfigurationEntryDao configurationEntryDao;

    @Inject
    private ApplicationConfiguration configuration;

    @Inject
    private SpaceGetter spaceGetter;

    protected MtaDescriptorPropertiesResolver getMtaDescriptorPropertiesResolver(HandlerFactory factory, Platform platform,
        SystemParameters systemParameters, ConfigurationEntryDao dao, BiFunction<String, String, String> spaceIdSupplier,
        CloudTarget cloudTarget) {
        return new MtaDescriptorPropertiesResolver(factory, platform, systemParameters, spaceIdSupplier, dao, cloudTarget, configuration);
    }

    protected UserProvidedResourceResolver getUserProvidedResourceResolver(DeploymentDescriptor descriptor, HandlerFactory handlerFactory,
        Platform platform, ResourceTypeFinder resourceHelper) {
        return handlerFactory.getUserProvidedResourceResolver(resourceHelper, descriptor, platform);
    }

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.RESOLVING_DESCRIPTOR_PROPERTIES);

            CloudControllerClient client = execution.getControllerClient();

            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(execution.getContext());
            Platform platform = StepsUtil.getPlatform(execution.getContext());
            ResourceTypeFinder resourceHelper = handlerFactory.getResourceTypeFinder(ResourceType.USER_PROVIDED_SERVICE.toString());
            platform.accept(resourceHelper);
            MtaDescriptorPropertiesResolver resolver = getMtaDescriptorPropertiesResolver(handlerFactory, platform,
                StepsUtil.getSystemParameters(execution.getContext()), configurationEntryDao, getSpaceIdSupplier(client),
                new CloudTarget(StepsUtil.getOrg(execution.getContext()), StepsUtil.getSpace(execution.getContext())));

            DeploymentDescriptor descriptor = resolver.resolve(StepsUtil.getUnresolvedDeploymentDescriptor(execution.getContext()));
            UserProvidedResourceResolver userProvidedServiceResolver = getUserProvidedResourceResolver(descriptor, handlerFactory, platform,
                resourceHelper);

            descriptor = userProvidedServiceResolver.resolve();

            // Merge DeploymentDescriptor and Platform
            handlerFactory.getPlatformMerger(platform)
                .mergeInto(descriptor);

            List<ConfigurationSubscription> subscriptions = resolver.getSubscriptions();
            StepsUtil.setSubscriptionsToCreate(execution.getContext(), subscriptions);
            XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(execution.getContext());

            resolveXsPlaceholders(descriptor, xsPlaceholderResolver, handlerFactory.getMajorVersion());

            StepsUtil.setDeploymentDescriptor(execution.getContext(), descriptor);
            // Set MTA modules in the context
            Set<String> mtaModules = CloudModelBuilderUtil.getModuleNames(descriptor);
            getStepLogger().debug("MTA Modules: {0}", mtaModules);
            StepsUtil.setMtaModules(execution.getContext(), mtaModules);

            getStepLogger().debug(com.sap.cloud.lm.sl.cf.core.message.Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR,
                secureSerializer.toJson(descriptor));
            getStepLogger().debug(Messages.DESCRIPTOR_PROPERTIES_RESOVED);

            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_RESOLVING_DESCRIPTOR_PROPERTIES);
            throw e;
        }
    }

    private void resolveXsPlaceholders(DeploymentDescriptor descriptor, XsPlaceholderResolver xsPlaceholderResolver, int majorVersion) {
        XsPlaceholderResolverInvoker resolverInvoker = new XsPlaceholderResolverInvoker(majorVersion, xsPlaceholderResolver);
        descriptor.accept(resolverInvoker);
    }

    protected BiFunction<String, String, String> getSpaceIdSupplier(CloudControllerClient client) {
        return (orgName, spaceName) -> new ClientHelper(client, spaceGetter).computeSpaceId(orgName, spaceName);
    }

}
