package com.sap.cloud.lm.sl.cf.core.helpers;

import static java.text.MessageFormat.format;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.common.ContentException;

public class MtaArchiveHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MtaArchiveHelper.class);

    // Attribute names:
    public static final String ATTR_MTA_RESOURCE = "MTA-Resource";
    public static final String ATTR_MTA_REQUIRES_DEPENDENCY = "MTA-Requires";
    public static final String ATTR_MTA_MODULE = "MTA-Module";
    private static final String ATTR_MTA_MODULES = "MTA-Modules";

    private final Manifest manifest;

    private Map<String, String> mtaArchiveResources;
    private Map<String, String> mtaArchiveModules;
    private Map<String, String> mtaArchiveRequiresDependencies;
    private Set<String> mtaModules;

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

    public Set<String> getMtaModules() {
        return mtaModules;
    }

    public void init() throws ContentException {
        // Determine MTA archive modules:
        mtaArchiveModules = getEntriesWithAttribute(ATTR_MTA_MODULE);

        // Determine MTA requires dependencies:
        mtaArchiveRequiresDependencies = getEntriesWithAttribute(ATTR_MTA_REQUIRES_DEPENDENCY);

        // Determine MTA archive resources:
        mtaArchiveResources = getEntriesWithAttribute(ATTR_MTA_RESOURCE);

        // Determine MTA modules:
        String mtaModulesValue = manifest.getMainAttributes().getValue(ATTR_MTA_MODULES);
        if (mtaModulesValue != null) {
            mtaModules = new HashSet<String>(Arrays.asList(mtaModulesValue.split(",\\s?")));
        } else {
            LOGGER.debug(format("No \"{0}\" entry specified in MTA manifest. Assuming all MTA modules are listed in the manifest",
                ATTR_MTA_MODULES));
            mtaModules = mtaArchiveModules.keySet();

        }
    }

    private Map<String, String> getEntriesWithAttribute(String attributeName) throws ContentException {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Attributes> entry : manifest.getEntries().entrySet()) {
            String attributeValue = entry.getValue().getValue(attributeName);
            if (attributeValue == null) {
                continue;
            }
            String fileName = entry.getKey();
            MtaPathValidator.validatePath(fileName);
            if (attributeName.equals(ATTR_MTA_MODULE)) {
                Arrays.asList(attributeValue.split(Constants.MODULE_SEPARATOR)).forEach(module -> result.put(module.trim(), fileName));
            } else {
                result.put(attributeValue, fileName);
            }
        }
        return result;
    }
}
