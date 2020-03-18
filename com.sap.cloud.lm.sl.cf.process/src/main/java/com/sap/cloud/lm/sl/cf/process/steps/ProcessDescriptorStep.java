package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorPropertiesResolver;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableMtaDescriptorPropertiesResolverContext;
import com.sap.cloud.lm.sl.cf.core.model.MtaDescriptorPropertiesResolverContext;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;

import liquibase.util.StringUtils;

public class ProcessDescriptorStep extends SyncFlowableStep {

    protected final SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    private ConfigurationEntryService configurationEntryService;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        DelegateExecution context = execution.getContext();
        getStepLogger().debug(Messages.RESOLVING_DESCRIPTOR_PROPERTIES);

        DeploymentDescriptor descriptor = execution.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
        MtaDescriptorPropertiesResolver resolver = getMtaDescriptorPropertiesResolver(context);

        descriptor = resolver.resolve(descriptor);

        List<ConfigurationSubscription> subscriptions = resolver.getSubscriptions();
        getStepLogger().debug(Messages.SUBSCRIPTIONS, secureSerializer.toJson(subscriptions));
        execution.setVariable(Variables.SUBSCRIPTIONS_TO_CREATE, subscriptions);

        execution.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, descriptor);
        // Set MTA modules in the context
        List<String> modulesForDeployment = StepsUtil.getModulesForDeployment(context);
        List<String> invalidModulesSpecifiedForDeployment = findInvalidModulesSpecifiedForDeployment(descriptor, modulesForDeployment);
        if (!invalidModulesSpecifiedForDeployment.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Messages.MODULES_0_SPECIFIED_FOR_DEPLOYMENT_ARE_NOT_PART_OF_DEPLOYMENT_DESCRIPTOR_MODULES,
                                                                 StringUtils.join(invalidModulesSpecifiedForDeployment, ", ")));
        }
        Set<String> mtaModules = getModuleNames(descriptor, modulesForDeployment);
        getStepLogger().debug("MTA Modules: {0}", mtaModules);
        execution.setVariable(Variables.MTA_MODULES, mtaModules);

        getStepLogger().debug(Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR, secureSerializer.toJson(descriptor));
        getStepLogger().debug(Messages.DESCRIPTOR_PROPERTIES_RESOLVED);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ExecutionWrapper execution) {
        return Messages.ERROR_RESOLVING_DESCRIPTOR_PROPERTIES;
    }

    protected MtaDescriptorPropertiesResolver getMtaDescriptorPropertiesResolver(DelegateExecution context) {
        return new MtaDescriptorPropertiesResolver(buildMtaDescriptorPropertiesResolverContext(context));
    }

    private MtaDescriptorPropertiesResolverContext buildMtaDescriptorPropertiesResolverContext(DelegateExecution context) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
        CloudTarget cloudTarget = new CloudTarget(StepsUtil.getOrg(context), StepsUtil.getSpace(context));
        String currentSpaceId = StepsUtil.getSpaceId(context);
        boolean useNamespacesForServices = StepsUtil.getUseNamespacesForService(context);
        boolean useNamespaces = StepsUtil.getUseNamespaces(context);
        boolean setIdleRoutes = StepsUtil.getUseIdleUris(context);

        return ImmutableMtaDescriptorPropertiesResolverContext.builder()
                                                              .handlerFactory(handlerFactory)
                                                              .cloudTarget(cloudTarget)
                                                              .currentSpaceId(currentSpaceId)
                                                              .hasUseNamespaces(useNamespaces)
                                                              .hasUserNamespacesForServices(useNamespacesForServices)
                                                              .shouldReserveTemporaryRoute(setIdleRoutes)
                                                              .configurationEntryService(configurationEntryService)
                                                              .applicationConfiguration(configuration)
                                                              .build();
    }

    private Set<String> getModuleNames(DeploymentDescriptor deploymentDescriptor, List<String> moduleNamesForDeployment) {
        if (moduleNamesForDeployment == null) {
            return deploymentDescriptor.getModules()
                                       .stream()
                                       .map(Module::getName)
                                       .collect(Collectors.toSet());
        }

        return new HashSet<>(moduleNamesForDeployment);
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

}
