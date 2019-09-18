package com.sap.cloud.lm.sl.cf.core.helpers.v3;

import static com.sap.cloud.lm.sl.cf.core.util.NameUtil.getIndexedName;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class ConfigurationReferenceResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationReferenceResolver {

    public ConfigurationReferenceResolver(ConfigurationEntryService configurationEntryService, ApplicationConfiguration configuration) {
        super(configurationEntryService, configuration);
    }

    @Override
    protected Resource asResource(ConfigurationEntry entry, Resource resource, int index, int entriesCount) {
        String indexedResourceName = getIndexedName(resource.getName(), index, entriesCount, RESOURCE_INDEX_DELIMITER);
        Map<String, Object> properties = mergeProperties(resource, entry);
        Map<String, Object> parameters = removeConfigurationParameters(resource.getParameters());
        return createResource().setName(indexedResourceName)
                               .setDescription(resource.getDescription())
                               .setProperties(properties)
                               .setParameters(parameters)
                               .setPropertiesMetadata(resource.getPropertiesMetadata())
                               .setParametersMetadata(resource.getParametersMetadata())
                               .setRequiredDependencies(resource.getRequiredDependencies());
    }

    @Override
    protected Resource createResource() {
        return Resource.createV3();
    }

}
