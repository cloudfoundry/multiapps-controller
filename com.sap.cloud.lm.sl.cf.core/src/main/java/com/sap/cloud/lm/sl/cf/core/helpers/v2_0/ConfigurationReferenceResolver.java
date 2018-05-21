package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import static com.sap.cloud.lm.sl.cf.core.util.NameUtil.getIndexedName;
import static com.sap.cloud.lm.sl.common.util.MapUtil.merge;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.v2_0.Resource;
import com.sap.cloud.lm.sl.mta.model.v2_0.Resource.ResourceBuilder;

public class ConfigurationReferenceResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationReferenceResolver {

    public ConfigurationReferenceResolver(ConfigurationEntryDao dao, ApplicationConfiguration configuration) {
        super(dao, configuration);
    }

    @Override
    protected Resource asResource(ConfigurationEntry entry, com.sap.cloud.lm.sl.mta.model.v1_0.Resource resource, int index,
        int entriesCount) throws ParsingException {
        return asResource(entry, (Resource) resource, index, entriesCount);
    }

    private Resource asResource(ConfigurationEntry entry, Resource resource, int index, int entriesCount) throws ParsingException {
        String indexedResourceName = getIndexedName(resource.getName(), index, entriesCount, RESOURCE_INDEX_DELIMITER);
        Map<String, Object> properties = mergeProperties(resource, entry);
        Map<String, Object> parameters = removeConfigurationParameters(resource.getParameters());
        ResourceBuilder builder = getResourceBuilder();
        builder.setName(indexedResourceName);
        builder.setDescription(resource.getDescription());
        builder.setProperties(properties);
        builder.setParameters(parameters);
        return builder.build();
    }

    @Override
    protected ResourceBuilder getResourceBuilder() {
        return new ResourceBuilder();
    }

    @Override
    protected Map<String, Object> mergeProperties(com.sap.cloud.lm.sl.mta.model.v1_0.Resource resource,
        ConfigurationEntry configurationEntry) throws ParsingException {
        return merge(JsonUtil.convertJsonToMap(configurationEntry.getContent()), resource.getProperties());
    }

}
