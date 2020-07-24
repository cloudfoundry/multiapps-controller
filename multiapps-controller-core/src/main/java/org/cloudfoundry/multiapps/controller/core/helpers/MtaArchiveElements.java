package org.cloudfoundry.multiapps.controller.core.helpers;

import java.util.HashMap;
import java.util.Map;

public class MtaArchiveElements {

    private final Map<String, String> modulesFileNames = new HashMap<>();
    private final Map<String, String> resourcesFileNames = new HashMap<>();
    private final Map<String, String> requiresFileNames = new HashMap<>();

    public void addModuleFileName(String moduleName, String fileName) {
        modulesFileNames.put(moduleName, fileName);
    }

    public String getModuleFileName(String moduleName) {
        return modulesFileNames.get(moduleName);
    }

    public void addResourceFileName(String resourceName, String fileName) {
        resourcesFileNames.put(resourceName, fileName);
    }

    public String getResourceFileName(String resourceName) {
        return resourcesFileNames.get(resourceName);
    }

    public void addRequiredDependencyFileName(String requiredDependencyName, String fileName) {
        requiresFileNames.put(requiredDependencyName, fileName);
    }

    public String getRequiredDependencyFileName(String requiredDependencyName) {
        return requiresFileNames.get(requiredDependencyName);
    }

}
