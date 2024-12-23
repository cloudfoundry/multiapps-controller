package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveHelper;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.SERVICE_CONFIG;

public class MtaArchiveContentResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MtaArchiveContentResolver.class);

    private final ContentLengthTracker sizeTracker;

    private final ExternalFileProcessor fileProcessor;

    private final MtaArchiveHelper helper;

    public MtaArchiveContentResolver(MtaArchiveHelper helper, ExternalFileProcessor fileProcessor, ContentLengthTracker sizeTracker) {
        this.helper = helper;
        this.fileProcessor = fileProcessor;
        this.sizeTracker = sizeTracker;
    }

    public void resolveMtaArchiveFilesInDescriptor(String appArchiveId, DeploymentDescriptor descriptor) {
        resolveResourcesContent(appArchiveId, descriptor);
        resolveRequiresDependenciesContent(appArchiveId, descriptor);
        long totalSizeOfResolvedEntries = sizeTracker.getTotalSize();
        LOGGER.debug(MessageFormat.format(Messages.TOTAL_SIZE_OF_ALL_RESOLVED_CONTENT_0, totalSizeOfResolvedEntries));
    }

    private void resolveResourcesContent(String appArchiveId, DeploymentDescriptor descriptor) {
        Map<String, List<String>> resourceFileAttributes = helper.getResourceFileAttributes();
        for (var entry : resourceFileAttributes.entrySet()) {
            Map<String, Object> parametersFromFile = fileProcessor.processFileContent(appArchiveId, entry);
            mergeResourcesFromFile(entry, parametersFromFile, descriptor);
        }
    }

    private void mergeResourcesFromFile(Map.Entry<String, List<String>> entry, Map<String, Object> parametersFromFile,
                                        DeploymentDescriptor descriptor) {
        List<Resource> resolvedResources = new ArrayList<>();
        for (Resource resource : descriptor.getResources()) {
            if (entryMatchesResource(entry, resource)) {
                setResourceParametersFromFile(resource, parametersFromFile);
            }
            resolvedResources.add(resource);
        }
        descriptor.setResources(resolvedResources);
    }

    private boolean entryMatchesResource(Map.Entry<String, List<String>> entry, Resource resource) {
        return entry.getValue()
                    .stream()
                    .anyMatch(s -> s.equals(resource.getName()));

    }

    private void setResourceParametersFromFile(Resource resource, Map<String, Object> parametersFromFile) {
        Map<String, Object> mergedProperties = mergeConfiguration(resource.getParameters(), parametersFromFile);
        resource.setParameters(mergedProperties);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeConfiguration(Map<String, Object> parameters, Map<String, Object> resolvedResources) {
        Map<String, Object> resourcesCopy = new HashMap<>(parameters);
        resourcesCopy.putIfAbsent(SERVICE_CONFIG, new HashMap<String, Object>());

        Map<String, Object> serviceCreationParameters = (Map<String, Object>) resourcesCopy.getOrDefault(SERVICE_CONFIG,
                                                                                                         Collections.emptyMap());

        resolvedResources.forEach(serviceCreationParameters::putIfAbsent);
        return resourcesCopy;
    }

    private void resolveRequiresDependenciesContent(String appArchiveId, DeploymentDescriptor descriptor) {
        Map<String, List<String>> dependencyFileAttributes = helper.getRequiresDependenciesFileAttributes();
        for (var entry : dependencyFileAttributes.entrySet()) {
            Map<String, Object> parametersFromFile = fileProcessor.processFileContent(appArchiveId, entry);
            mergeMtaRequiresDependencyInModules(entry, parametersFromFile, descriptor);
        }
    }

    private void mergeMtaRequiresDependencyInModules(Map.Entry<String, List<String>> entry, Map<String, Object> parametersFromFile,
                                                     DeploymentDescriptor descriptor) {
        List<Module> resolvedModules = new ArrayList<>();
        for (Module module : descriptor.getModules()) {
            resolveRequiredDependencyFileParameters(module, entry, parametersFromFile);
            resolvedModules.add(module);
        }
        descriptor.setModules(resolvedModules);
    }

    private void resolveRequiredDependencyFileParameters(Module module, Map.Entry<String, List<String>> entry,
                                                         Map<String, Object> parametersFromFile) {
        for (String requiresPath : entry.getValue()) {
            String[] splitEntry = requiresPath.split(Constants.MTA_ELEMENT_SEPARATOR);
            String requiresDependencyModuleName = splitEntry[0];
            String requiresDependencyName = splitEntry[1];
            if (module.getName()
                      .equals(requiresDependencyModuleName)) {
                RequiredDependency requiredDependency = getRequiredDependencyForModule(module, requiresDependencyName);
                Map<String, Object> mergedRequiredDependencyParameters = mergeConfiguration(requiredDependency.getParameters(),
                                                                                            parametersFromFile);
                requiredDependency.setParameters(mergedRequiredDependencyParameters);
            }
        }
    }

    private RequiredDependency getRequiredDependencyForModule(Module module, String requiresDependencyName) {
        return module.getRequiredDependencies()
                     .stream()
                     .filter(r -> r.getName()
                                   .equals(requiresDependencyName))
                     .findFirst()
                     .orElseThrow(
                         () -> new SLException(Messages.ERROR_COULD_NOT_FIND_REQUIRED_DEPENDENCY_0_FOR_MODULE_1, requiresDependencyName,
                                               module.getName()));
    }

}
