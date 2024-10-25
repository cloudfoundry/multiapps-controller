package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaDescriptorPropertiesResolver;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableMtaDescriptorPropertiesResolverContext;
import org.cloudfoundry.multiapps.controller.core.model.MtaDescriptorPropertiesResolverContext;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.NamespaceGlobalParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("processDescriptorStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessDescriptorStep extends SyncFlowableStep {

    @Inject
    private ConfigurationEntryService configurationEntryService;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.RESOLVING_DESCRIPTOR_PROPERTIES);

        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
        MtaDescriptorPropertiesResolver resolver = getMtaDescriptorPropertiesResolver(context, descriptor);

        descriptor = resolver.resolve(descriptor);

        List<ConfigurationSubscription> subscriptions = resolver.getSubscriptions();
        getStepLogger().debug(Messages.SUBSCRIPTIONS, SecureSerialization.toJson(subscriptions));
        context.setVariable(Variables.SUBSCRIPTIONS_TO_CREATE, subscriptions);

        setDynamicResolvableParametersIfAbsent(context, resolver);
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, descriptor);
        // Set MTA modules in the context
        List<String> modulesForDeployment = context.getVariable(Variables.MODULES_FOR_DEPLOYMENT);
        List<String> invalidModulesSpecifiedForDeployment = findInvalidModulesSpecifiedForDeployment(descriptor, modulesForDeployment);
        if (!invalidModulesSpecifiedForDeployment.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Messages.MODULES_0_SPECIFIED_FOR_DEPLOYMENT_ARE_NOT_PART_OF_DEPLOYMENT_DESCRIPTOR_MODULES,
                                                                 String.join(", ", invalidModulesSpecifiedForDeployment)));
        }
        Set<String> mtaModules = getModuleNames(descriptor, modulesForDeployment);
        getStepLogger().debug(Messages.MTA_MODULES, mtaModules);
        context.setVariable(Variables.MTA_MODULES, mtaModules);

        getStepLogger().debug(Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR, SecureSerialization.toJson(descriptor));
        getStepLogger().debug(Messages.DESCRIPTOR_PROPERTIES_RESOLVED);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_RESOLVING_DESCRIPTOR_PROPERTIES;
    }

    protected MtaDescriptorPropertiesResolver getMtaDescriptorPropertiesResolver(ProcessContext context, DeploymentDescriptor descriptor) {
        return new MtaDescriptorPropertiesResolver(buildMtaDescriptorPropertiesResolverContext(context, descriptor));
    }

    private MtaDescriptorPropertiesResolverContext buildMtaDescriptorPropertiesResolverContext(ProcessContext context,
                                                                                               DeploymentDescriptor descriptor) {
        CloudHandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());
        CloudTarget cloudTarget = new CloudTarget(context.getVariable(Variables.ORGANIZATION_NAME),
                                                  context.getVariable(Variables.SPACE_NAME));
        String currentSpaceId = context.getVariable(Variables.SPACE_GUID);
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);
        Boolean applyNamespaceAppNamesProcessVariable = context.getVariable(Variables.APPLY_NAMESPACE_APP_NAMES);
        Boolean applyNamespaceServiceNamesProcessVariable = context.getVariable(Variables.APPLY_NAMESPACE_SERVICE_NAMES);
        Boolean applyNamespaceAppRoutesProcessVariable = context.getVariable(Variables.APPLY_NAMESPACE_APP_ROUTES);
        Boolean applyNamespaceAsSuffixProcessVariable = context.getVariable(Variables.APPLY_NAMESPACE_AS_SUFFIX);

        boolean setIdleRoutes = context.getVariable(Variables.USE_IDLE_URIS);

        NamespaceGlobalParameters namespaceGlobalParameters = new NamespaceGlobalParameters(descriptor);

        return ImmutableMtaDescriptorPropertiesResolverContext.builder()
                                                              .handlerFactory(handlerFactory)
                                                              .cloudTarget(cloudTarget)
                                                              .currentSpaceId(currentSpaceId)
                                                              .namespace(namespace)
                                                              .applyNamespaceAppNamesGlobalLevel(namespaceGlobalParameters.getApplyNamespaceAppNamesParameter())
                                                              .applyNamespaceServiceNamesGlobalLevel(namespaceGlobalParameters.getApplyNamespaceServiceNamesParameter())
                                                              .applyNamespaceAppRoutesGlobalLevel(namespaceGlobalParameters.getApplyNamespaceAppRoutesParameter())
                                                              .applyNamespaceAsSuffixGlobalLevel(namespaceGlobalParameters.getApplyNamespaceAsSuffix())
                                                              .applyNamespaceAsSuffixProcessVariable(applyNamespaceAsSuffixProcessVariable)
                                                              .applyNamespaceAppNamesProcessVariable(applyNamespaceAppNamesProcessVariable)
                                                              .applyNamespaceServiceNamesProcessVariable(applyNamespaceServiceNamesProcessVariable)
                                                              .applyNamespaceAppRoutesProcessVariable(applyNamespaceAppRoutesProcessVariable)
                                                              .shouldReserveTemporaryRoute(setIdleRoutes)
                                                              .configurationEntryService(configurationEntryService)
                                                              .applicationConfiguration(configuration)
                                                              .build();
    }

    private void setDynamicResolvableParametersIfAbsent(ProcessContext context, MtaDescriptorPropertiesResolver resolver) {
        if (context.getVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETERS)
                   .isEmpty()) {
            Set<DynamicResolvableParameter> dynamicResolvableParameters = resolver.getDynamicResolvableParameters();
            context.setVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETERS, dynamicResolvableParameters);
        }
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
