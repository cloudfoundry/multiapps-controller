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
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ServiceType;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorPropertiesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolverInvoker;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ResourceTypeFinder;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.UserProvidedResourceResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ZdmHelper;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ProcessType;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("processDescriptorStep")
public class ProcessDescriptorStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessDescriptorStep.class);

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
        logActivitiTask(context, LOGGER);

        try {
            debug(context, Messages.RESOLVING_DESCRIPTOR_PROPERTIES, LOGGER);

            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);

            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
            Target target = StepsUtil.getTarget(context);
            Platform platform = StepsUtil.getPlatform(context);
            ResourceTypeFinder resourceHelper = handlerFactory.getResourceTypeFinder(ServiceType.USER_PROVIDED.toString());
            platform.accept(resourceHelper);

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

            ProcessType processType = StepsUtil.getProcessType(context);
            if (!processType.equals(ProcessType.BLUE_GREEN_DEPLOY)) {
                validateZdmModeParameter(context);
            }

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

    private void validateZdmModeParameter(DelegateExecution context) throws ContentException {
        DeploymentDescriptor descriptor = StepsUtil.getDeploymentDescriptor(context);
        int majorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION);
        int minorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION);
        if ((new ZdmHelper()).existsZdmMarker(descriptor, majorSchemaVersion, minorSchemaVersion)) {
            throw new SLException(Messages.ERROR_ZDM_MODE_PARAMETER);
        }
    }
}
