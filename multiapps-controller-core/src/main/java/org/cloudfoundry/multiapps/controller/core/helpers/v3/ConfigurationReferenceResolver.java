package org.cloudfoundry.multiapps.controller.core.helpers.v3;

import static org.cloudfoundry.multiapps.controller.core.util.NameUtil.getIndexedName;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class ConfigurationReferenceResolver extends org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationReferenceResolver {

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
