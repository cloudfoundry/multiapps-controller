package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.function.BiFunction;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ServiceType;
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
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("processDescriptorStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessDescriptorStep extends AbstractXS2ProcessStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("processDescriptorTask").displayName("Process Descriptor Step").description(
            "Process Descriptor Step").build();
    }

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
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();

        try {
            getStepLogger().debug(Messages.RESOLVING_DESCRIPTOR_PROPERTIES);

            CloudFoundryOperations client = getCloudFoundryClient(context);

            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
            Target target = StepsUtil.getTarget(context);
            Platform platform = StepsUtil.getPlatform(context);
            ResourceTypeFinder resourceHelper = handlerFactory.getResourceTypeFinder(ServiceType.USER_PROVIDED.toString());
            platform.accept(resourceHelper);
            getStepLogger().debug(Messages.TARGET, target);
            MtaDescriptorPropertiesResolver resolver = getMtaDescriptorPropertiesResolver(handlerFactory, platform, target,
                StepsUtil.getSystemParameters(context), configurationEntryDao, getSpaceIdSupplier(client),
                new CloudTarget(StepsUtil.getOrg(context), StepsUtil.getSpace(context)));

            DeploymentDescriptor descriptor = resolver.resolve(StepsUtil.getUnresolvedDeploymentDescriptor(context));
            UserProvidedResourceResolver userProvidedServiceResolver = getUserProvidedResourceResolver(descriptor, handlerFactory, target,
                platform, resourceHelper);

            descriptor = userProvidedServiceResolver.resolve();

            // Merge DeploymentDescriptor and Target
            handlerFactory.getTargetMerger(target).mergeInto(descriptor);

            // Merge DeploymentDescriptor and Platform
            handlerFactory.getPlatformMerger(platform).mergeInto(descriptor);

            List<ConfigurationSubscription> subscriptions = resolver.getSubscriptions();
            StepsUtil.setSubscriptionsToCreate(context, subscriptions);
            XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(context);
            resolveXsPlaceholders(descriptor, xsPlaceholderResolver, handlerFactory.getMajorVersion());

            StepsUtil.setDeploymentDescriptor(context, descriptor);

            getStepLogger().debug(com.sap.cloud.lm.sl.cf.core.message.Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR,
                secureSerializer.toJson(descriptor));
            getStepLogger().debug(Messages.DESCRIPTOR_PROPERTIES_RESOVED);

            return ExecutionStatus.SUCCESS;
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
