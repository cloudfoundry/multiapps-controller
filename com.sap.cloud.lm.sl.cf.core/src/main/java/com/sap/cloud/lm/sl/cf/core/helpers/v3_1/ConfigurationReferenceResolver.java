package com.sap.cloud.lm.sl.cf.core.helpers.v3_1;

import static com.sap.cloud.lm.sl.cf.core.util.NameUtil.getIndexedName;
import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource.Builder;

public class ConfigurationReferenceResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v3_0.ConfigurationReferenceResolver {

    public ConfigurationReferenceResolver(ConfigurationEntryDao dao, ApplicationConfiguration configuration) {
        super(dao, configuration);
    }

    @Override
    protected Resource asResource(ConfigurationEntry entry, com.sap.cloud.lm.sl.mta.model.v1_0.Resource resource, int index,
        int entriesCount) throws ParsingException {
        return asResource(entry, (Resource) resource, index, entriesCount);
    }

    private Resource asResource(ConfigurationEntry entry, Resource resource, int index, int entriesCount) throws ParsingException {
        Resource resource3_1 = cast(resource);
        String indexedResourceName = getIndexedName(resource.getName(), index, entriesCount, RESOURCE_INDEX_DELIMITER);
        Map<String, Object> properties = mergeProperties(resource, entry);
        Map<String, Object> parameters = removeConfigurationParameters(resource.getParameters());
        Builder builder = getResourceBuilder();
        builder.setName(indexedResourceName);
        builder.setDescription(resource.getDescription());
        builder.setProperties(properties);
        builder.setParameters(parameters);
        builder.setPropertiesMetadata(resource3_1.getPropertiesMetadata());
        builder.setParametersMetadata(resource3_1.getParametersMetadata());
        builder.setRequiredDependencies(resource3_1.getRequiredDependencies3_1());
        return builder.build();
    }

    @Override
    protected Builder getResourceBuilder() {
        return new Builder();
    }
}
