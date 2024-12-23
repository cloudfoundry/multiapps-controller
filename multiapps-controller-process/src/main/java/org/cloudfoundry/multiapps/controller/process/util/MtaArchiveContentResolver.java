package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveHelper;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
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

public class MtaArchiveContentResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MtaArchiveContentResolver.class);

    private final DeploymentDescriptor descriptor;

    private final ApplicationConfiguration configuration;

    private final ContentLengthTracker sizeTracker;

    private final ExternalFileProcessor fileProcessor;

    public MtaArchiveContentResolver(DeploymentDescriptor descriptor, ApplicationConfiguration configuration, ExternalFileProcessor fileProcessor, ContentLengthTracker sizeTracker) {
        this.descriptor = descriptor;
        this.configuration = configuration;
        this.fileProcessor = fileProcessor;
        this.sizeTracker = sizeTracker;
    }

    public void resolveMtaArchiveFilesInDescriptor(String space, String appArchiveId, MtaArchiveHelper helper) {
        resolveResourcesContent(space, appArchiveId, helper);
        resolveRequiresDependenciesContent(space, appArchiveId, helper);
        long totalSizeOfResolvedEntries = sizeTracker.getTotalSize();
        LOGGER.debug(MessageFormat.format(Messages.TOTAL_SIZE_OF_ALL_RESOLVED_CONTENT_0, totalSizeOfResolvedEntries));
        if (totalSizeOfResolvedEntries > configuration.getMaxResolvedExternalContentSize()) {
            throw new SLException(Messages.ERROR_RESOLVED_FILE_CONTENT_IS_0_WHICH_IS_LARGER_THAN_MAX_1, totalSizeOfResolvedEntries,
                                  configuration.getMaxResolvedExternalContentSize());
        }
    }

    private void resolveResourcesContent(String space, String appArchiveId, MtaArchiveHelper helper) {
        Map<String, List<String>> resourceFileAttributes = helper.getResourceFileAttributes();
        for (var entry : resourceFileAttributes.entrySet()) {
            Map<String, Object> fileContentForEntry = fileProcessor.processFileContent(space, appArchiveId, entry.getKey());
            mergeResourcesFromFile(entry, fileContentForEntry);
        }
    }

    private void mergeResourcesFromFile(Map.Entry<String, List<String>> entry, Map<String, Object> fileContent) {
        List<Resource> list = new ArrayList<>();
        for (Resource resource : descriptor.getResources()) {
            if (entryMatchesResource(entry, resource)) {
                setResourceParametersFromFile(resource, fileContent);
                sizeTracker.incrementFileSize();
            }
            list.add(resource);
        }
        descriptor.setResources(list);
    }

    private boolean entryMatchesResource(Map.Entry<String, List<String>> entry, Resource resource) {
        return entry.getValue()
                    .stream()
                    .anyMatch(s -> s.equals(resource.getName()));

    }

    private void setResourceParametersFromFile(Resource resource, Map<String, Object> fileContent) {
        Map<String, Object> mergedProperties = mergeConfiguration(resource.getParameters(), fileContent);
        resource.setParameters(mergedProperties);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeConfiguration(Map<String, Object> parameters, Map<String, Object> resolvedResources) {
        Map<String, Object> resourcesCopy = new HashMap<>(parameters);
        resourcesCopy.putIfAbsent("config", new HashMap<String, Object>());

        Map<String, Object> serviceKeyCreationParameters = (Map<String, Object>) resourcesCopy.getOrDefault("config", Collections.emptyMap());

        resolvedResources.forEach(serviceKeyCreationParameters::putIfAbsent);
        return resourcesCopy;
    }

    private void resolveRequiresDependenciesContent(String space, String appArchiveId, MtaArchiveHelper helper) {
        Map<String, List<String>> dependencyFileAttributes = helper.getRequiresDependenciesFileAttributes();
        for (var entry : dependencyFileAttributes.entrySet()) {
            Map<String, Object> fileContentForEntry = fileProcessor.processFileContent(space, appArchiveId, entry.getKey());
            mergeMtaRequiresDependencyInModules(entry, fileContentForEntry);
        }
    }

    private void mergeMtaRequiresDependencyInModules(Map.Entry<String, List<String>> entry, Map<String, Object> fileContent) {
        List<Module> list = new ArrayList<>();
        for (Module module : descriptor.getModules()) {
            resolveRequiredDependencyFileParameters(module, entry, fileContent);
            list.add(module);
        }
        descriptor.setModules(list);
    }

    private void resolveRequiredDependencyFileParameters(Module module, Map.Entry<String, List<String>> entry, Map<String, Object> fileContent) {
        for (String requiresPath : entry.getValue()) {
            String[] splitEntry = requiresPath.split(Constants.MTA_ELEMENT_SEPARATOR);
            String requiresDependencyModuleName = splitEntry[0];
            String requiresDependencyName = splitEntry[1];
            if (module.getName()
                      .equals(requiresDependencyModuleName)) {
                RequiredDependency requiredDependency = getRequiredDependencyForModule(module, requiresDependencyName);
                Map<String, Object> mergedRequiredDependencyParameters = mergeConfiguration(requiredDependency.getParameters(), fileContent);
                requiredDependency.setParameters(mergedRequiredDependencyParameters);
                sizeTracker.incrementFileSize();
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
                         () -> new SLException(Messages.ERROR_COULD_NOT_FIND_REQUIRED_DEPENDENCY_0_FOR_MODULE_1, requiresDependencyName, module.getName()));
    }

}
