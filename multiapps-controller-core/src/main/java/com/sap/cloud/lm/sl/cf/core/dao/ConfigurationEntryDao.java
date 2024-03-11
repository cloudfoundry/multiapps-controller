package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.ConfigurationEntryDto;
import com.sap.cloud.lm.sl.cf.core.filters.VersionFilter;
import com.sap.cloud.lm.sl.cf.core.filters.VisibilityFilter;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;

@Component
public class ConfigurationEntryDao {

    private static final BiFunction<ConfigurationEntry, String, Boolean> VERSION_FILTER = new VersionFilter();
    private static final BiFunction<ConfigurationEntry, List<CloudTarget>, Boolean> VISIBILITY_FILTER = new VisibilityFilter();
    @Inject
    protected ConfigurationEntryDtoDao dao;

    public List<ConfigurationEntry> find(String nid, String id, String version, CloudTarget target, Map<String, Object> requiredProperties,
                                         String mtaId, List<CloudTarget> cloudTargets) {
        return filter(toConfigurationEntries(dao.find(nid, id, target, requiredProperties, mtaId)), version, cloudTargets);
    }

    public List<ConfigurationEntry> find(String nid, String id, String version, CloudTarget target, Map<String, Object> requiredProperties,
                                         String mtaId) {
        return find(nid, id, version, target, requiredProperties, mtaId, null);
    }

    private List<ConfigurationEntry> filter(List<ConfigurationEntry> entries, String version, List<CloudTarget> cloudTargets) {
        return entries.stream()
                      .filter(entry -> VERSION_FILTER.apply(entry, version))
                      .filter(entry -> VISIBILITY_FILTER.apply(entry, cloudTargets))
                      .collect(Collectors.toList());
    }

    private List<ConfigurationEntry> toConfigurationEntries(List<ConfigurationEntryDto> dtos) {
        return dtos.stream()
                   .map(ConfigurationEntryDto::toConfigurationEntry)
                   .collect(Collectors.toList());
    }

    public ConfigurationEntry update(long id, ConfigurationEntry entry) {
        return dao.update(id, new ConfigurationEntryDto(entry))
                  .toConfigurationEntry();
    }

    public ConfigurationEntry find(long id) {
        return dao.find(id)
                  .toConfigurationEntry();
    }

    public List<ConfigurationEntry> find(String spaceGuid) {
        return toConfigurationEntries(dao.find(spaceGuid));
    }

    public void remove(long id) {
        dao.remove(id);
    }

    public List<ConfigurationEntry> removeAll(List<ConfigurationEntry> configurationEntries) {
        for (ConfigurationEntry configurationEntry : configurationEntries) {
            dao.remove(configurationEntry.getId());
        }
        return configurationEntries;
    }

    public ConfigurationEntry add(ConfigurationEntry entry) {
        return dao.add(new ConfigurationEntryDto(entry))
                  .toConfigurationEntry();
    }

    public boolean exists(long id) {
        return dao.exists(id);
    }

    public List<ConfigurationEntry> findAll() {
        return toConfigurationEntries(dao.findAll());
    }

}
