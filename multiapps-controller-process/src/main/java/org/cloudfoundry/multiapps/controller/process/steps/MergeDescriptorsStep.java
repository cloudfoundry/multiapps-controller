package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaDescriptorMerger;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableBackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorBackupService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.NamespaceGlobalParameters;
import org.cloudfoundry.multiapps.controller.process.util.UnsupportedParameterFinder;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionModule;
import org.cloudfoundry.multiapps.mta.model.ExtensionProvidedDependency;
import org.cloudfoundry.multiapps.mta.model.ExtensionRequiredDependency;
import org.cloudfoundry.multiapps.mta.model.ExtensionResource;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.ParametersContainer;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.cloudfoundry.multiapps.mta.model.PropertiesContainer;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.resolvers.ReferenceContainer;
import org.cloudfoundry.multiapps.mta.resolvers.ReferencesFinder;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("mergeDescriptorsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MergeDescriptorsStep extends SyncFlowableStep {

    private static final String SECURE_ID = "__mta.secure";

    @Inject
    private DescriptorBackupService descriptorBackupService;

    @Inject
    private UnsupportedParameterFinder unsupportedParameterFinder;

    protected MtaDescriptorMerger getMtaDescriptorMerger(CloudHandlerFactory factory, Platform platform) {
        return new MtaDescriptorMerger(factory, platform, getStepLogger());
    }

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.MERGING_DESCRIPTORS);
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        List<ExtensionDescriptor> extensionDescriptors = context.getVariable(Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN);
        CloudHandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());
        Platform platform = configuration.getPlatform();
        Set<String> parameterNamesToBeCensored = collectSecureParameterKeys(extensionDescriptors);
        MultiValuedMap<String, String> parametersNameValueMapFromDescriptorAndExtensionDescriptors = getParametersNameValueMapFromDeploymentDescriptor(
            deploymentDescriptor);
        parametersNameValueMapFromDescriptorAndExtensionDescriptors.putAll(
            getParametersNameValueMapFromExtensionDescriptors(extensionDescriptors));
        Set<String> nestedParameterNamesToBeCensored = getNestedParameterNamesToBeCensored(
            parametersNameValueMapFromDescriptorAndExtensionDescriptors,
            parameterNamesToBeCensored);
        parameterNamesToBeCensored.addAll(nestedParameterNamesToBeCensored);
        context.setVariable(Variables.SECURE_EXTENSION_DESCRIPTOR_PARAMETER_NAMES,
                            parameterNamesToBeCensored);

        DeploymentDescriptor descriptor = getMtaDescriptorMerger(handlerFactory, platform).merge(deploymentDescriptor,
                                                                                                 extensionDescriptors,
                                                                                                 parameterNamesToBeCensored.stream()
                                                                                                                           .toList());
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);

        warnForUnsupportedParameters(descriptor);

        backupDeploymentDescriptor(context, descriptor);
        getStepLogger().debug(Messages.DESCRIPTORS_MERGED);
        return StepPhase.DONE;
    }

    private Set<String> getNestedParameterNamesToBeCensored(MultiValuedMap<String, String> parameterNameValueMap,
                                                            Set<String> parameterNamesToBeCensored) {
        Set<String> nestedParameterNamesToBeCensored = new HashSet<>();
        for (Map.Entry<String, Collection<String>> parameterEntryInStringType : parameterNameValueMap.asMap()
                                                                                                     .entrySet()) {
            List<String> entryValuesToString = parameterEntryInStringType.getValue()
                                                                         .stream()
                                                                         .map(String::toString)
                                                                         .toList();
            for (String complexValue : entryValuesToString) {
                for (String nameToBeCensored : parameterNamesToBeCensored) {
                    if (complexValue.contains(nameToBeCensored)) {
                        nestedParameterNamesToBeCensored.add(parameterEntryInStringType.getKey());
                    }
                }
            }
        }
        return nestedParameterNamesToBeCensored;
    }

    private MultiValuedMap<String, String> getParametersNameValueMapFromExtensionDescriptors(
        List<ExtensionDescriptor> extensionDescriptors) {
        MultiValuedMap<String, String> parametersNameValueMapFromExtensionDescriptors = new ArrayListValuedHashMap<>();

        for (ExtensionDescriptor currentExtensionDescriptor : extensionDescriptors) {
            Map<String, Object> currentExtensionDescriptorParameters = currentExtensionDescriptor.getParameters();
            if (currentExtensionDescriptorParameters != null) {
                parametersNameValueMapFromExtensionDescriptors.putAll(getParametersStringCastedValue(currentExtensionDescriptor));
            }

            List<ExtensionModule> extensionModules = currentExtensionDescriptor.getModules();
            if (extensionModules != null) {
                for (ExtensionModule extensionModule : extensionModules) {
                    getParametersAndPropertiesPerExtensionModule(parametersNameValueMapFromExtensionDescriptors, extensionModule, false,
                                                                 true, false);
                    getParametersAndPropertiesPerExtensionModule(parametersNameValueMapFromExtensionDescriptors, extensionModule, true,
                                                                 false, false);
                    getParametersAndPropertiesPerExtensionModule(parametersNameValueMapFromExtensionDescriptors, extensionModule, false,
                                                                 false, true);
                }
            }

            List<ExtensionResource> extensionResources = currentExtensionDescriptor.getResources();
            if (extensionResources != null) {
                for (ExtensionResource extensionResource : extensionResources) {
                    getParametersAndPropertiesPerExtensionResource(parametersNameValueMapFromExtensionDescriptors, extensionResource, false,
                                                                   true);

                    if (currentExtensionDescriptor.getMajorSchemaVersion() >= 3) {
                        getParametersAndPropertiesPerExtensionResource(parametersNameValueMapFromExtensionDescriptors, extensionResource,
                                                                       true, false);
                    }

                }
            }
        }

        return parametersNameValueMapFromExtensionDescriptors;
    }

    private MultiValuedMap<String, String> getParametersNameValueMapFromDeploymentDescriptor(DeploymentDescriptor descriptor) {
        MultiValuedMap<String, String> parametersNameValueMapFromDeploymentDescriptor = new ArrayListValuedHashMap<>();
        Map<String, Object> descriptorParameters = descriptor.getParameters();
        if (descriptorParameters != null) {
            parametersNameValueMapFromDeploymentDescriptor.putAll(getParametersStringCastedValue(descriptor));
        }

        List<Module> modules = descriptor.getModules();
        if (modules != null) {
            for (Module module : modules) {
                getParametersAndPropertiesPerModule(parametersNameValueMapFromDeploymentDescriptor, module, false, true, false);
                getParametersAndPropertiesPerModule(parametersNameValueMapFromDeploymentDescriptor, module, true, false, false);
                getParametersAndPropertiesPerModule(parametersNameValueMapFromDeploymentDescriptor, module, false, false, true);
            }
        }
        
        List<Resource> resources = descriptor.getResources();
        if (resources != null) {
            for (Resource resource : resources) {
                getParametersAndPropertiesPerResource(parametersNameValueMapFromDeploymentDescriptor, resource, false, true);

                if (descriptor.getMajorSchemaVersion() >= 3) {
                    getParametersAndPropertiesPerResource(parametersNameValueMapFromDeploymentDescriptor, resource, true, false);
                }

            }
        }

        return parametersNameValueMapFromDeploymentDescriptor;
    }

    private void warnForUnsupportedParameters(DeploymentDescriptor descriptor) {
        List<ReferenceContainer> references = new ReferencesFinder().getAllReferences(descriptor);
        Map<String, List<String>> unsupportedParameters = unsupportedParameterFinder.findUnsupportedParameters(descriptor,
                                                                                                               references);
        if (!unsupportedParameters.isEmpty()) {
            getStepLogger().warn(MessageFormat.format(Messages.PARAMETERS_0_ARE_NOT_SUPPORTED_OR_REFERENCED_BY_ANY_OTHER_ENTITIES,
                                                      unsupportedParameters));
        }
    }

    private void backupDeploymentDescriptor(ProcessContext context, DeploymentDescriptor descriptor) {
        boolean shouldBackupPreviousVersion = context.getVariable(Variables.SHOULD_BACKUP_PREVIOUS_VERSION);
        if (!shouldBackupPreviousVersion) {
            return;
        }
        checkForUnsupportedParameters(context, descriptor, shouldBackupPreviousVersion);

        String spaceGuid = context.getVariable(Variables.SPACE_GUID);
        String mtaId = descriptor.getId();
        String mtaNamespace = context.getVariable(Variables.MTA_NAMESPACE);
        String mtaVersion = descriptor.getVersion();
        List<BackupDescriptor> backupDescriptors = descriptorBackupService.createQuery()
                                                                          .mtaId(mtaId)
                                                                          .spaceId(spaceGuid)
                                                                          .namespace(mtaNamespace)
                                                                          .mtaVersion(mtaVersion)
                                                                          .list();
        if (backupDescriptors.isEmpty()) {
            descriptorBackupService.add(ImmutableBackupDescriptor.builder()
                                                                 .descriptor(descriptor)
                                                                 .mtaId(mtaId)
                                                                 .mtaVersion(mtaVersion)
                                                                 .spaceId(spaceGuid)
                                                                 .namespace(mtaNamespace)
                                                                 .build());
        }
    }

    private void checkForUnsupportedParameters(ProcessContext context, DeploymentDescriptor descriptor,
                                               boolean shouldBackupPreviousVersion) {
        NamespaceGlobalParameters namespaceGlobalParameters = new NamespaceGlobalParameters(descriptor);
        if (shouldBackupPreviousVersion && (Objects.requireNonNullElse(context.getVariable(Variables.APPLY_NAMESPACE_AS_SUFFIX), false)
            || namespaceGlobalParameters.getApplyNamespaceAsSuffix())) {
            throw new UnsupportedOperationException(Messages.BACKUP_PREVIOUS_VERSION_FLAG_AND_APPLY_NAMESPACE_AS_SUFFIX_NOT_SUPPORTED);
        }
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_MERGING_DESCRIPTORS;
    }

    private Set<String> collectSecureParameterKeys(List<ExtensionDescriptor> extensionDescriptors) {
        Set<String> resultKeysNames = new HashSet<>();

        if (extensionDescriptors == null) {
            return resultKeysNames;
        }

        for (ExtensionDescriptor currentExtensionDescriptor : extensionDescriptors) {
            if (currentExtensionDescriptor != null && (currentExtensionDescriptor.getId()).equals(SECURE_ID)) {
                Map<String, Object> parameters = currentExtensionDescriptor.getParameters();
                if (parameters != null) {
                    resultKeysNames.addAll(parameters.keySet());
                }
            }
        }

        return resultKeysNames;
    }

    private Map<String, String> getParametersStringCastedValue(ParametersContainer parametersContainer) {
        return parametersContainer.getParameters()
                                  .entrySet()
                                  .stream()
                                  .collect(Collectors.toMap(Map.Entry::getKey,
                                                            currentParameter -> Objects.toString(currentParameter.getValue(), "")));
    }

    private Map<String, String> getPropertiesStringCastedValue(PropertiesContainer propertiesContainer) {
        return propertiesContainer.getProperties()
                                  .entrySet()
                                  .stream()
                                  .collect(Collectors.toMap(Map.Entry::getKey,
                                                            currentProperty -> Objects.toString(currentProperty.getValue(), "")));
    }

    private void getParametersAndPropertiesPerResource(MultiValuedMap<String, String> multiValuedMap, Resource resource, boolean isRequired,
                                                       boolean isWhole) {
        if (isRequired) {
            for (RequiredDependency requiredDependency : resource.getRequiredDependencies()) {
                multiValuedMap.putAll(getParametersStringCastedValue(requiredDependency));
                multiValuedMap.putAll(getPropertiesStringCastedValue(requiredDependency));
            }
        }
        if (isWhole) {
            multiValuedMap.putAll(getParametersStringCastedValue(resource));
            multiValuedMap.putAll(getPropertiesStringCastedValue(resource));
        }
    }

    private void getParametersAndPropertiesPerModule(MultiValuedMap<String, String> multiValuedMap, Module module, boolean isRequired,
                                                     boolean isWhole, boolean isProvided) {
        if (isRequired) {
            for (RequiredDependency requiredDependency : module.getRequiredDependencies()) {
                multiValuedMap.putAll(getParametersStringCastedValue(requiredDependency));
                multiValuedMap.putAll(getPropertiesStringCastedValue(requiredDependency));
            }
        }
        if (isProvided) {
            for (ProvidedDependency providedDependency : module.getProvidedDependencies()) {
                multiValuedMap.putAll(getParametersStringCastedValue(providedDependency));
                multiValuedMap.putAll(getPropertiesStringCastedValue(providedDependency));
            }
        }
        if (isWhole) {
            multiValuedMap.putAll(getParametersStringCastedValue(module));
            multiValuedMap.putAll(getPropertiesStringCastedValue(module));
        }
    }

    private void getParametersAndPropertiesPerExtensionResource(MultiValuedMap<String, String> multiValuedMap,
                                                                ExtensionResource extensionResource,
                                                                boolean isRequired, boolean isWhole) {
        if (isRequired) {
            for (ExtensionRequiredDependency extensionRequiredDependency : extensionResource.getRequiredDependencies()) {
                multiValuedMap.putAll(getParametersStringCastedValue(extensionRequiredDependency));
                multiValuedMap.putAll(getPropertiesStringCastedValue(extensionRequiredDependency));
            }
        }
        if (isWhole) {
            multiValuedMap.putAll(getParametersStringCastedValue(extensionResource));
            multiValuedMap.putAll(getPropertiesStringCastedValue(extensionResource));
        }
    }

    private void getParametersAndPropertiesPerExtensionModule(MultiValuedMap<String, String> multiValuedMap,
                                                              ExtensionModule extensionModule, boolean isRequired,
                                                              boolean isWhole, boolean isProvided) {
        if (isRequired) {
            for (ExtensionRequiredDependency extensionRequiredDependency : extensionModule.getRequiredDependencies()) {
                multiValuedMap.putAll(getParametersStringCastedValue(extensionRequiredDependency));
                multiValuedMap.putAll(getPropertiesStringCastedValue(extensionRequiredDependency));
            }
        }
        if (isProvided) {
            for (ExtensionProvidedDependency extensionProvidedDependency : extensionModule.getProvidedDependencies()) {
                multiValuedMap.putAll(getParametersStringCastedValue(extensionProvidedDependency));
                multiValuedMap.putAll(getPropertiesStringCastedValue(extensionProvidedDependency));
            }
        }
        if (isWhole) {
            multiValuedMap.putAll(getParametersStringCastedValue(extensionModule));
            multiValuedMap.putAll(getPropertiesStringCastedValue(extensionModule));
        }
    }

}
