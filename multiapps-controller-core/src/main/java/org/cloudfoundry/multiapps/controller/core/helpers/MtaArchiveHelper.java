package org.cloudfoundry.multiapps.controller.core.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.cloudfoundry.multiapps.controller.core.Constants;

public class MtaArchiveHelper {

    // Attribute names:
    public static final String ATTR_MTA_RESOURCE = "MTA-Resource";
    public static final String ATTR_MTA_REQUIRES_DEPENDENCY = "MTA-Requires";
    public static final String ATTR_MTA_MODULE = "MTA-Module";

    private final Manifest manifest;

    private Map<String, String> mtaArchiveResources;
    private Map<String, String> mtaArchiveModules;
    private Map<String, String> mtaArchiveRequiresDependencies;

    public MtaArchiveHelper(Manifest manifest) {
        this.manifest = manifest;
    }

    public Map<String, String> getMtaArchiveResources() {
        return mtaArchiveResources;
    }

    public Map<String, String> getMtaArchiveModules() {
        return mtaArchiveModules;
    }

    public Map<String, String> getMtaRequiresDependencies() {
        return mtaArchiveRequiresDependencies;
    }

    public void init() {
        // Determine MTA archive modules:
        mtaArchiveModules = getEntriesWithAttribute(ATTR_MTA_MODULE);

        // Determine MTA requires dependencies:
        mtaArchiveRequiresDependencies = getEntriesWithAttribute(ATTR_MTA_REQUIRES_DEPENDENCY);

        // Determine MTA archive resources:
        mtaArchiveResources = getEntriesWithAttribute(ATTR_MTA_RESOURCE);
    }

    private Map<String, String> getEntriesWithAttribute(String attributeName) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Attributes> entry : manifest.getEntries()
                                                           .entrySet()) {
            String attributeValue = entry.getValue()
                                         .getValue(attributeName);
            if (attributeValue == null) {
                continue;
            }
            String fileName = entry.getKey();
            MtaPathValidator.validatePath(fileName);
            for (String mtaEntity : attributeValue.split(Constants.MANIFEST_MTA_ENTITY_SEPARATOR)) {
                result.put(mtaEntity.trim(), fileName);
            }
        }
        return result;
    }
}
