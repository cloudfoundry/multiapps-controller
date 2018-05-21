package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import static com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil.findConfigurationEntries;
import static com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil.getGlobalConfigTarget;
import static com.sap.cloud.lm.sl.cf.core.util.NameUtil.getIndexedName;
import static com.sap.cloud.lm.sl.common.util.MapUtil.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.model.json.PropertiesAdapterFactory;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource.ResourceBuilder;

public class ConfigurationReferenceResolver {

    protected static final String RESOURCE_INDEX_DELIMITER = ".";

    protected ConfigurationEntryDao dao;
    protected ApplicationConfiguration configuration;

    public ConfigurationReferenceResolver(ConfigurationEntryDao dao, ApplicationConfiguration configuration) {
        this.dao = dao;
        this.configuration = configuration;
    }

    public List<Resource> resolve(Resource resource, ConfigurationFilter filter, CloudTarget cloudTarget) throws ParsingException {
        CloudTarget globalConfigTarget = getGlobalConfigTarget(configuration);
        return asResources(findConfigurationEntries(dao, filter, getCloudTargetsList(cloudTarget), globalConfigTarget), resource);
    }

    private List<CloudTarget> getCloudTargetsList(CloudTarget target) {
        return target == null ? null : Arrays.asList(target);
    }

    protected List<Resource> asResources(List<ConfigurationEntry> entries, Resource resource) throws ParsingException {
        List<Resource> result = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            result.add(asResource(entries.get(i), resource, i, entries.size()));
        }
        return result;
    }

    protected Resource asResource(ConfigurationEntry entry, Resource resource, int index, int entriesCount) throws ParsingException {
        String indexedResourceName = getIndexedName(resource.getName(), index, entriesCount, RESOURCE_INDEX_DELIMITER);
        Map<String, Object> properties = mergeProperties(resource, entry);
        ResourceBuilder builder = getResourceBuilder();
        builder.setName(indexedResourceName);
        builder.setDescription(resource.getDescription());
        builder.setGroups(resource.getGroups());
        builder.setProperties(properties);
        return builder.build();
    }

    protected ResourceBuilder getResourceBuilder() {
        return new ResourceBuilder();
    }

    protected Map<String, Object> removeConfigurationParameters(Map<String, Object> resourcePropertiesMap) {
        Map<String, Object> result = new TreeMap<>(resourcePropertiesMap);
        result.keySet()
            .removeAll(SupportedParameters.CONFIGURATION_REFERENCE_PARAMETERS);
        return result;
    }

    protected Map<String, Object> mergeProperties(Resource resource, ConfigurationEntry configurationEntry) throws ParsingException {
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new PropertiesAdapterFactory())
            .create();
        Map<String, Object> contentMap = gson.fromJson(configurationEntry.getContent(), new TypeToken<Map<String, Object>>() {
        }.getType());
        return merge(contentMap, removeConfigurationParameters(resource.getProperties()));
    }

}
