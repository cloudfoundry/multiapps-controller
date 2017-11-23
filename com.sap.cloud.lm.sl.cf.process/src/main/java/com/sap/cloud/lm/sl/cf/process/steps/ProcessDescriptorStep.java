package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudFoundryOperations;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ResourceType;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorPropertiesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolverInvoker;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public class ProcessDescriptorStep extends SyncActivitiStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    private ConfigurationEntryDao configurationEntryDao;

    protected MtaDescriptorPropertiesResolver getMtaDescriptorPropertiesResolver(HandlerFactory factory, Platform platform, Target target,
        SystemParameters systemParameters, ConfigurationEntryDao dao, BiFunction<String, String, String> spaceIdSupplier,
        CloudTarget cloudTarget) {
        return new MtaDescriptorPropertiesResolver(factory, platform, target, systemParameters, spaceIdSupplier, dao, cloudTarget);
    }

    protected UserProvidedResourceResolver getUserProvidedResourceResolver(DeploymentDescriptor descriptor, HandlerFactory handlerFactory,
        Target target, Platform platform, ResourceTypeFinder resourceHelper) {
        return handlerFactory.getUserProvidedResourceResolver(resourceHelper, descriptor, target, platform);
    }

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        getStepLogger().logActivitiTask();

        try {
            getStepLogger().debug(Messages.RESOLVING_DESCRIPTOR_PROPERTIES);

            CloudFoundryOperations client = execution.getCloudFoundryClient();

            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(execution.getContext());
            Target target = StepsUtil.getTarget(execution.getContext());
            Platform platform = StepsUtil.getPlatform(execution.getContext());
            ResourceTypeFinder resourceHelper = handlerFactory.getResourceTypeFinder(ResourceType.USER_PROVIDED_SERVICE.toString());
            platform.accept(resourceHelper);
            getStepLogger().debug(Messages.TARGET, target);
            MtaDescriptorPropertiesResolver resolver = getMtaDescriptorPropertiesResolver(handlerFactory, platform, target,
                StepsUtil.getSystemParameters(execution.getContext()), configurationEntryDao, getSpaceIdSupplier(client),
                new CloudTarget(StepsUtil.getOrg(execution.getContext()), StepsUtil.getSpace(execution.getContext())));

            DeploymentDescriptor descriptor = resolver.resolve(StepsUtil.getUnresolvedDeploymentDescriptor(execution.getContext()));
            UserProvidedResourceResolver userProvidedServiceResolver = getUserProvidedResourceResolver(descriptor, handlerFactory, target,
                platform, resourceHelper);

            descriptor = userProvidedServiceResolver.resolve();

            // Merge DeploymentDescriptor and Target
            handlerFactory.getTargetMerger(target).mergeInto(descriptor);

            // Merge DeploymentDescriptor and Platform
            handlerFactory.getPlatformMerger(platform).mergeInto(descriptor);

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

    private void resolveXsPlaceholders(DeploymentDescriptor descriptor, XsPlaceholderResolver xsPlaceholderResolver, int majorVersion)
        throws SLException {
        XsPlaceholderResolverInvoker resolverInvoker = new XsPlaceholderResolverInvoker(majorVersion, xsPlaceholderResolver);
        descriptor.accept(resolverInvoker);
    }

    protected BiFunction<String, String, String> getSpaceIdSupplier(CloudFoundryOperations client) {
        return (orgName, spaceName) -> new ClientHelper(client).computeSpaceId(orgName, spaceName);
    }

}
