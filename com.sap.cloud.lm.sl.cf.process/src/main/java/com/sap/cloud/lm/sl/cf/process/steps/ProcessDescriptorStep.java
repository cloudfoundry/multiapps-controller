package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;

import liquibase.util.StringUtils;

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
            List<String> modulesForDeployment = StepsUtil.getModulesForDeployment(execution.getContext());
            List<String> invalidModulesSpecifiedForDeployment = findInvalidModulesSpecifiedForDeployment(descriptor, modulesForDeployment);
            if (!invalidModulesSpecifiedForDeployment.isEmpty()) {
                throw new IllegalStateException(
                    MessageFormat.format(Messages.MODULES_0_SPECIFIED_FOR_DEPLOYMENT_ARE_NOT_PART_OF_DEPLOYMENT_DESCRIPTOR_MODULES,
                        StringUtils.join(invalidModulesSpecifiedForDeployment, ", ")));
            }
            Set<String> mtaModules = getModuleNames(descriptor, modulesForDeployment);
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

    private Set<String> getModuleNames(DeploymentDescriptor deploymentDescriptor, List<String> moduleNamesForDeployment) {
        if (moduleNamesForDeployment.isEmpty()) {
            return deploymentDescriptor.getModules2()
                .stream()
                .map(Module::getName)
                .collect(Collectors.toSet());
        }

        return moduleNamesForDeployment.stream()
            .collect(Collectors.toSet());
    }

    private List<String> findInvalidModulesSpecifiedForDeployment(DeploymentDescriptor descriptor, List<String> modulesForDeployment) {
        if (modulesForDeployment.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> deploymentDescriptorModuleNames = descriptor.getModules2()
            .stream()
            .map(Module::getName)
            .collect(Collectors.toSet());
        return modulesForDeployment.stream()
            .filter(moduleSpecifiedForDeployment -> !deploymentDescriptorModuleNames.contains(moduleSpecifiedForDeployment))
            .collect(Collectors.toList());
    }

    private void resolveXsPlaceholders(DeploymentDescriptor descriptor, XsPlaceholderResolver xsPlaceholderResolver, int majorVersion) {
        XsPlaceholderResolverInvoker resolverInvoker = new XsPlaceholderResolverInvoker(majorVersion, xsPlaceholderResolver);
        descriptor.accept(resolverInvoker);
    }

    protected BiFunction<String, String, String> getSpaceIdSupplier(CloudControllerClient client) {
        return (orgName, spaceName) -> new ClientHelper(client, spaceGetter).computeSpaceId(orgName, spaceName);
    }

}
