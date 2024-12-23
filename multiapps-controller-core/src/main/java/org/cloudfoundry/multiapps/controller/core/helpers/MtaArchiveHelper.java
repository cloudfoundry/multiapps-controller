package org.cloudfoundry.multiapps.controller.core.helpers;

import org.cloudfoundry.multiapps.controller.core.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class MtaArchiveHelper {

    // Attribute names:
    public static final String ATTR_MTA_RESOURCE = "MTA-Resource";
    public static final String ATTR_MTA_REQUIRES_DEPENDENCY = "MTA-Requires";
    public static final String ATTR_MTA_MODULE = "MTA-Module";

    private static final String CONTENT_FILE_TYPE_JSON = "application/json";
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
        processEntries(attributeName, (attributeValue, fileName) -> fillMapWithEntries(attributeValue, fileName, result), manifest.getEntries());
        return result;
    }

    private void fillMapWithEntries(String attributeValue, String fileName, Map<String, String> result) {
        for (String mtaEntity : attributeValue.split(Constants.MANIFEST_MTA_ENTITY_SEPARATOR)) {
            result.put(mtaEntity.trim(), fileName);
        }
    }

    private void processEntries(String attributeName, BiConsumer<String, String> consumer, Map<String, Attributes> manifestEntries) {
        for (Map.Entry<String, Attributes> entry : manifestEntries.entrySet()) {
            String attributeValue = entry.getValue()
                                         .getValue(attributeName);
            if (attributeValue == null) {
                continue;
            }
            String fileName = entry.getKey();
            MtaPathValidator.validatePath(fileName);
            consumer.accept(attributeValue, fileName);
        }
    }

    public Map<String, List<String>> getResourceFileAttributes() {
        return getFilesWithEntityList(ATTR_MTA_RESOURCE, getAttributesWithJSONContentType());
    }

    public Map<String, List<String>> getRequiresDependenciesFileAttributes() {
        return getFilesWithEntityList(ATTR_MTA_REQUIRES_DEPENDENCY, getAttributesWithJSONContentType());
    }

    private Map<String, Attributes> getAttributesWithJSONContentType() {
        return manifest.getEntries()
                       .entrySet()
                       .stream()
                       .filter(entry -> entry.getValue()
                                             .containsValue(CONTENT_FILE_TYPE_JSON))
                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, List<String>> getFilesWithEntityList(String attributeName, Map<String, Attributes> manifestEntries) {
        Map<String, List<String>> result = new HashMap<>();
        processEntries(attributeName, (attributeValue, fileName) -> fillMapFilesWithEntityList(attributeValue, fileName, result), manifestEntries);
        return result;
    }

    private void fillMapFilesWithEntityList(String attributeValue, String fileName, Map<String, List<String>> result) {
        List<String> entities = new ArrayList<>();
        for (String mtaEntity : attributeValue.split(Constants.MANIFEST_MTA_ENTITY_SEPARATOR)) {
            entities.add(mtaEntity.trim());
        }
        result.put(fileName, entities);
    }

}
