package org.cloudfoundry.multiapps.controller.core.helpers.v2;

import static org.cloudfoundry.multiapps.controller.core.util.ConfigurationEntriesUtil.findConfigurationEntries;
import static org.cloudfoundry.multiapps.controller.core.util.ConfigurationEntriesUtil.getGlobalConfigTarget;
import static org.cloudfoundry.multiapps.controller.core.util.NameUtil.getIndexedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.filters.ConfigurationFilter;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class ConfigurationReferenceResolver {

    protected static final String RESOURCE_INDEX_DELIMITER = ".";

    protected final ConfigurationEntryService configurationEntryService;
    protected final ApplicationConfiguration configuration;

    public ConfigurationReferenceResolver(ConfigurationEntryService configurationEntryService, ApplicationConfiguration configuration) {
        this.configurationEntryService = configurationEntryService;
        this.configuration = configuration;
    }

    public List<Resource> resolve(Resource resource, ConfigurationFilter filter, CloudTarget cloudTarget) {
        CloudTarget globalConfigTarget = getGlobalConfigTarget(configuration);
        return asResources(findConfigurationEntries(configurationEntryService, filter, getCloudTargetsList(cloudTarget),
                                                    globalConfigTarget),
                           resource);
    }

    private List<CloudTarget> getCloudTargetsList(CloudTarget target) {
        return target == null ? null : Collections.singletonList(target);
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
        return MapUtil.merge(JsonUtil.convertJsonToMap(configurationEntry.getContent()), resource.getProperties());
    }
}
