package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.function.BiFunction;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorPropertiesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolverInvoker;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("processDescriptorStep")
public class ProcessDescriptorStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessDescriptorStep.class);

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    public static StepMetadata getMetadata() {
        return new StepMetadata("processDescriptorStepTask", "Process Descriptor Step", "Process Descriptor Step");
    }

    @Inject
    private ConfigurationEntryDao configurationEntryDao;

    protected MtaDescriptorPropertiesResolver getMtaDescriptorPropertiesResolver(HandlerFactory factory, TargetPlatformType platformType,
        TargetPlatform platform, SystemParameters systemParameters, ConfigurationEntryDao dao,
        BiFunction<String, String, String> spaceIdSupplier) {
        return new MtaDescriptorPropertiesResolver(factory, platformType, platform, systemParameters, spaceIdSupplier, dao);
    }

    protected UserProvidedResourceResolver getUserProvidedResourceResolver(DeploymentDescriptor descriptor, HandlerFactory handlerFactory,
        TargetPlatform platform, TargetPlatformType platformType, ResourceTypeFinder resourceHelper) {
        return handlerFactory.getUserProvidedResourceResolver(resourceHelper, descriptor, platform, platformType);
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        try {
            info(context, Messages.RESOLVING_DESCRIPTOR_PROPERTIES, LOGGER);

            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);

            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
            TargetPlatform platform = StepsUtil.getPlatform(context);
            TargetPlatformType platformType = StepsUtil.getPlatformType(context);
            ResourceTypeFinder resourceHelper = handlerFactory.getResourceTypeFinder(
                CloudModelBuilder.ServiceType.USER_PROVIDED.toString());
            platformType.accept(resourceHelper);

            MtaDescriptorPropertiesResolver resolver = getMtaDescriptorPropertiesResolver(handlerFactory, platformType, platform,
                StepsUtil.getSystemParameters(context), configurationEntryDao, getSpaceIdSupplier(client));

            DeploymentDescriptor descriptor = resolver.resolve(StepsUtil.getDeploymentDescriptor(context));
            UserProvidedResourceResolver userProvidedServiceResolver = getUserProvidedResourceResolver(descriptor, handlerFactory, platform,
                platformType, resourceHelper);

            descriptor = userProvidedServiceResolver.resolve();

            // Merge DeploymentDescriptor and TargetPlatform
            handlerFactory.getTargetPlatformMerger(platform).mergeInto(descriptor);

            // Merge DeploymentDescriptor and TargetPlatformType
            handlerFactory.getTargetPlatformTypeMerger(platformType).mergeInto(descriptor);

            List<ConfigurationSubscription> subscriptions = resolver.getSubscriptions();
            StepsUtil.setSubscriptionsToCreate(context, subscriptions);
            XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(context);
            resolveXsPlaceholders(descriptor, xsPlaceholderResolver, handlerFactory.getMajorVersion());

            StepsUtil.setDeploymentDescriptor(context, descriptor);
            debug(context,
                format(com.sap.cloud.lm.sl.cf.core.message.Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR, secureSerializer.toJson(descriptor)),
                LOGGER);
            debug(context, Messages.DESCRIPTOR_PROPERTIES_RESOVED, LOGGER);

            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_RESOLVING_DESCRIPTOR_PROPERTIES, e, LOGGER);
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
