package com.sap.cloud.lm.sl.cf.core.helpers.v3;

import static com.sap.cloud.lm.sl.cf.core.util.NameUtil.getIndexedName;
import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.mta.model.v3.Resource;

public class ConfigurationReferenceResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationReferenceResolver {

    public ConfigurationReferenceResolver(ConfigurationEntryDao dao, ApplicationConfiguration configuration) {
        super(dao, configuration);
    }

    @Override
    protected Resource asResource(ConfigurationEntry entry, com.sap.cloud.lm.sl.mta.model.v1.Resource resource, int index,
        int entriesCount) {
        return asResource(entry, (Resource) resource, index, entriesCount);
    }

    private Resource asResource(ConfigurationEntry entry, Resource resource, int index, int entriesCount) {
        Resource resource3 = cast(resource);
        String indexedResourceName = getIndexedName(resource.getName(), index, entriesCount, RESOURCE_INDEX_DELIMITER);
        Map<String, Object> properties = mergeProperties(resource, entry);
        Map<String, Object> parameters = removeConfigurationParameters(resource.getParameters());
        Resource.Builder builder = getResourceBuilder();
        builder.setName(indexedResourceName);
        builder.setDescription(resource.getDescription());
        builder.setProperties(properties);
        builder.setParameters(parameters);
        builder.setPropertiesMetadata(resource3.getPropertiesMetadata());
        builder.setParametersMetadata(resource3.getParametersMetadata());
        builder.setRequiredDependencies3(resource3.getRequiredDependencies3());
        return builder.build();
    }

    @Override
    protected Resource.Builder getResourceBuilder() {
        return new Resource.Builder();
    }

}
