package org.cloudfoundry.multiapps.controller.core.helpers.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.cloudfoundry.multiapps.mta.model.Version;

import static org.cloudfoundry.multiapps.controller.core.util.ConfigurationEntriesUtil.findConfigurationEntries;
import static org.cloudfoundry.multiapps.controller.core.util.ConfigurationEntriesUtil.getGlobalConfigTarget;
import static org.cloudfoundry.multiapps.controller.core.util.NameUtil.getIndexedName;

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
        List<ConfigurationEntry> entries = findConfigurationEntries(configurationEntryService, filter, getCloudTargetsList(cloudTarget),
                                                                    globalConfigTarget);
        return asResources(reduceRedundantEntries(entries), resource);
    }

    // An unpinned reference can match several entries for one provided dependency, for example while a provider
    // redeploys and the new-version entry lands before the old one is deleted. Drop the duplicates that carry
    // identical content, then keep only the newest version among entries for the same provider
    // (nid/id/namespace/target). Keep entries that differ on another key or on content, so the caller still sees
    // real ambiguity.
    protected List<ConfigurationEntry> reduceRedundantEntries(List<ConfigurationEntry> entries) {
        if (entries.size() <= 1) {
            return entries;
        }
        List<ConfigurationEntry> reduced = collapseEntriesWithIdenticalContent(entries);
        return keepNewestVersionPerProvider(reduced);
    }

    private List<ConfigurationEntry> collapseEntriesWithIdenticalContent(List<ConfigurationEntry> entries) {
        Map<String, Object> firstContent = parseContent(entries.getFirst());
        boolean allIdentical = entries.stream()
                                      .allMatch(entry -> Objects.equals(parseContent(entry), firstContent));
        return allIdentical ? Collections.singletonList(entries.getFirst()) : entries;
    }

    private List<ConfigurationEntry> keepNewestVersionPerProvider(List<ConfigurationEntry> entries) {
        if (entries.size() <= 1) {
            return entries;
        }
        Map<ProviderIdentity, ConfigurationEntry> newestByProvider = new LinkedHashMap<>();
        for (ConfigurationEntry entry : entries) {
            newestByProvider.merge(ProviderIdentity.from(entry), entry, ConfigurationReferenceResolver::newerEntry);
        }
        return new ArrayList<>(newestByProvider.values());
    }

    private Map<String, Object> parseContent(ConfigurationEntry entry) {
        return entry.getContent() == null ? null : JsonUtil.convertJsonToMap(entry.getContent());
    }

    // Identity of a configuration entry's provided dependency, ignoring version
    private record ProviderIdentity(String nid, String id, String namespace, String targetOrg, String targetSpace) {
        static ProviderIdentity from(ConfigurationEntry entry) {
            CloudTarget target = entry.getTargetSpace() == null ? new CloudTarget() : entry.getTargetSpace();
            return new ProviderIdentity(entry.getProviderNid(), entry.getProviderId(), entry.getProviderNamespace(),
                                        target.getOrganizationName(), target.getSpaceName());
        }
    }

    private static ConfigurationEntry newerEntry(ConfigurationEntry existing, ConfigurationEntry candidate) {
        Version existingVersion = existing.getProviderVersion();
        Version candidateVersion = candidate.getProviderVersion();
        if (existingVersion == null || candidateVersion == null) {
            return existing;
        }
        return candidateVersion.compareTo(existingVersion) > 0 ? candidate : existing;
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
