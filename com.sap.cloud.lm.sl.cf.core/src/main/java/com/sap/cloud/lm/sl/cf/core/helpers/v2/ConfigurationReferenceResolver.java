package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import static com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil.findConfigurationEntries;
import static com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil.getGlobalConfigTarget;
import static com.sap.cloud.lm.sl.cf.core.util.NameUtil.getIndexedName;
import static com.sap.cloud.lm.sl.common.util.MapUtil.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class ConfigurationReferenceResolver {

    protected static final String RESOURCE_INDEX_DELIMITER = ".";

    protected ConfigurationEntryService configurationEntryService;
    protected ApplicationConfiguration configuration;
    protected Map<String, ResolvedConfigurationReference> resolvedReferences = new TreeMap<>();

    public ConfigurationReferenceResolver(ConfigurationEntryService configurationEntryService, ApplicationConfiguration configuration) {
        this.configurationEntryService = configurationEntryService;
        this.configuration = configuration;
    }

    public List<Resource> resolve(Resource resource, ConfigurationFilter filter, CloudTarget cloudTarget) {
        CloudTarget globalConfigTarget = getGlobalConfigTarget(configuration);
        return asResources(findConfigurationEntries(configurationEntryService, filter, getCloudTargetsList(cloudTarget), globalConfigTarget), resource);
    }

    private List<CloudTarget> getCloudTargetsList(CloudTarget target) {
        return target == null ? null : Arrays.asList(target);
    }

    protected List<Resource> asResources(List<ConfigurationEntry> entries, Resource resource) {
        List<Resource> result = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            result.add(asResource(entries.get(i), resource, i, entries.size()));
        }
        return result;
    }

    protected Resource asResource(ConfigurationEntry entry, Resource resource, int index, int entriesCount) {
        String indexedResourceName = getIndexedName(resource.getName(), index, entriesCount, RESOURCE_INDEX_DELIMITER);
        Map<String, Object> properties = mergeProperties(resource, entry);
        Map<String, Object> parameters = removeConfigurationParameters(resource.getParameters());
        return createResource().setName(indexedResourceName)
                               .setDescription(resource.getDescription())
                               .setProperties(properties)
                               .setParameters(parameters);
    }

    protected Map<String, Object> removeConfigurationParameters(Map<String, Object> resourcePropertiesMap) {
        Map<String, Object> result = new TreeMap<>(resourcePropertiesMap);
        result.keySet()
              .removeAll(SupportedParameters.CONFIGURATION_REFERENCE_PARAMETERS);
        return result;
    }

    protected Resource createResource() {
        return Resource.createV2();
    }

    protected Map<String, Object> mergeProperties(Resource resource, ConfigurationEntry configurationEntry) {
        return merge(JsonUtil.convertJsonToMap(configurationEntry.getContent()), resource.getProperties());
    }
}
