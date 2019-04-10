package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorPropertiesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolverInvoker;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;

import liquibase.util.StringUtils;

public class ProcessDescriptorStep extends SyncFlowableStep {

    protected SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    private ConfigurationEntryDao configurationEntryDao;

    @Inject
    private ApplicationConfiguration configuration;

    protected MtaDescriptorPropertiesResolver getMtaDescriptorPropertiesResolver(HandlerFactory factory, ConfigurationEntryDao dao,
        BiFunction<String, String, String> spaceIdSupplier, CloudTarget cloudTarget, boolean useNamespaces,
        boolean useNamespacesForServices) {
        return new MtaDescriptorPropertiesResolver(factory, spaceIdSupplier, dao, cloudTarget, configuration, useNamespaces,
            useNamespacesForServices);
    }

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        DelegateExecution context = execution.getContext();
        try {
            getStepLogger().debug(Messages.RESOLVING_DESCRIPTOR_PROPERTIES);

            CloudControllerClient client = execution.getControllerClient();
            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);

            DeploymentDescriptor descriptor = StepsUtil.getDeploymentDescriptorWithSystemParameters(execution.getContext());
            boolean useNamespacesForServices = (boolean) context.getVariable(Constants.PARAM_USE_NAMESPACES_FOR_SERVICES);
            boolean useNamespaces = (boolean) context.getVariable(Constants.PARAM_USE_NAMESPACES);
            MtaDescriptorPropertiesResolver resolver = getMtaDescriptorPropertiesResolver(handlerFactory, configurationEntryDao,
                getSpaceIdSupplier(client), new CloudTarget(StepsUtil.getOrg(context), StepsUtil.getSpace(context)), useNamespaces,
                useNamespacesForServices);

            descriptor = resolver.resolve(descriptor);

            List<ConfigurationSubscription> subscriptions = resolver.getSubscriptions();
            StepsUtil.setSubscriptionsToCreate(context, subscriptions);
            XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(context);

            resolveXsPlaceholders(descriptor, xsPlaceholderResolver);

            StepsUtil.setCompleteDeploymentDescriptor(execution.getContext(), descriptor);
            // Set MTA modules in the context
            List<String> modulesForDeployment = StepsUtil.getModulesForDeployment(context);
            List<String> invalidModulesSpecifiedForDeployment = findInvalidModulesSpecifiedForDeployment(descriptor, modulesForDeployment);
            if (!invalidModulesSpecifiedForDeployment.isEmpty()) {
                throw new IllegalStateException(
                    MessageFormat.format(Messages.MODULES_0_SPECIFIED_FOR_DEPLOYMENT_ARE_NOT_PART_OF_DEPLOYMENT_DESCRIPTOR_MODULES,
                        StringUtils.join(invalidModulesSpecifiedForDeployment, ", ")));
            }
            Set<String> mtaModules = getModuleNames(descriptor, modulesForDeployment);
            getStepLogger().debug("MTA Modules: {0}", mtaModules);
            StepsUtil.setMtaModules(context, mtaModules);

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
        if (moduleNamesForDeployment == null) {
            return deploymentDescriptor.getModules()
                .stream()
                .map(Module::getName)
                .collect(Collectors.toSet());
        }

        return moduleNamesForDeployment.stream()
            .collect(Collectors.toSet());
    }

    private List<String> findInvalidModulesSpecifiedForDeployment(DeploymentDescriptor descriptor, List<String> modulesForDeployment) {
        if (CollectionUtils.isEmpty(modulesForDeployment)) {
            return Collections.emptyList();
        }
        Set<String> deploymentDescriptorModuleNames = descriptor.getModules()
            .stream()
            .map(Module::getName)
            .collect(Collectors.toSet());
        return modulesForDeployment.stream()
            .filter(moduleSpecifiedForDeployment -> !deploymentDescriptorModuleNames.contains(moduleSpecifiedForDeployment))
            .collect(Collectors.toList());
    }

    private void resolveXsPlaceholders(DeploymentDescriptor descriptor, XsPlaceholderResolver xsPlaceholderResolver) {
        XsPlaceholderResolverInvoker resolverInvoker = new XsPlaceholderResolverInvoker(xsPlaceholderResolver);
        descriptor.accept(resolverInvoker);
    }

    protected BiFunction<String, String, String> getSpaceIdSupplier(CloudControllerClient client) {
        return (orgName, spaceName) -> new ClientHelper(client).computeSpaceId(orgName, spaceName);
    }

}
