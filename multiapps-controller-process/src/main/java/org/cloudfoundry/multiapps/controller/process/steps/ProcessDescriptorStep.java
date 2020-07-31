package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaDescriptorPropertiesResolver;
import org.cloudfoundry.multiapps.controller.core.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableMtaDescriptorPropertiesResolverContext;
import org.cloudfoundry.multiapps.controller.core.model.MtaDescriptorPropertiesResolverContext;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
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
        MtaDescriptorPropertiesResolver resolver = getMtaDescriptorPropertiesResolver(context);

        descriptor = resolver.resolve(descriptor);

        List<ConfigurationSubscription> subscriptions = resolver.getSubscriptions();
        getStepLogger().debug(Messages.SUBSCRIPTIONS, SecureSerialization.toJson(subscriptions));
        context.setVariable(Variables.SUBSCRIPTIONS_TO_CREATE, subscriptions);

        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, descriptor);
        // Set MTA modules in the context
        List<String> modulesForDeployment = context.getVariable(Variables.MODULES_FOR_DEPLOYMENT);
        List<String> invalidModulesSpecifiedForDeployment = findInvalidModulesSpecifiedForDeployment(descriptor, modulesForDeployment);
        if (!invalidModulesSpecifiedForDeployment.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Messages.MODULES_0_SPECIFIED_FOR_DEPLOYMENT_ARE_NOT_PART_OF_DEPLOYMENT_DESCRIPTOR_MODULES,
                                                                 String.join(", ", invalidModulesSpecifiedForDeployment)));
        }
        Set<String> mtaModules = getModuleNames(descriptor, modulesForDeployment);
        getStepLogger().debug("MTA Modules: {0}", mtaModules);
        context.setVariable(Variables.MTA_MODULES, mtaModules);

        getStepLogger().debug(Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR, SecureSerialization.toJson(descriptor));
        getStepLogger().debug(Messages.DESCRIPTOR_PROPERTIES_RESOLVED);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_RESOLVING_DESCRIPTOR_PROPERTIES;
    }

    protected MtaDescriptorPropertiesResolver getMtaDescriptorPropertiesResolver(ProcessContext context) {
        return new MtaDescriptorPropertiesResolver(buildMtaDescriptorPropertiesResolverContext(context));
    }

    private MtaDescriptorPropertiesResolverContext buildMtaDescriptorPropertiesResolverContext(ProcessContext context) {
        CloudHandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());
        CloudTarget cloudTarget = new CloudTarget(context.getVariable(Variables.ORGANIZATION_NAME),
                                                  context.getVariable(Variables.SPACE_NAME));
        String currentSpaceId = context.getVariable(Variables.SPACE_GUID);
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);
        boolean applyNamespace = context.getVariable(Variables.APPLY_NAMESPACE);
        boolean setIdleRoutes = context.getVariable(Variables.USE_IDLE_URIS);

        return ImmutableMtaDescriptorPropertiesResolverContext.builder()
                                                              .handlerFactory(handlerFactory)
                                                              .cloudTarget(cloudTarget)
                                                              .currentSpaceId(currentSpaceId)
                                                              .namespace(namespace)
                                                              .applyNamespace(applyNamespace)
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
