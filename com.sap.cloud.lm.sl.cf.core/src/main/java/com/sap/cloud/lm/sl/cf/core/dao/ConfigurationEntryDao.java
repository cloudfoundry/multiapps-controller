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
public class ConfigurationEntryDao extends AbstractDao<ConfigurationEntry, ConfigurationEntryDto, Long> {

    @Inject
    protected ConfigurationEntryDtoDao dao;

    private static final BiFunction<ConfigurationEntry, String, Boolean> VERSION_FILTER = new VersionFilter();
    private static final BiFunction<ConfigurationEntry, List<CloudTarget>, Boolean> VISIBILITY_FILTER = new VisibilityFilter();

    public boolean exists(long id) {
        return dao.exists(id);
    }

    public List<ConfigurationEntry> find(String nid, String id, String version, CloudTarget target, Map<String, Object> requiredProperties,
        String mtaId, List<CloudTarget> cloudTargets) {
        return filter(fromDtos(dao.find(nid, id, target, requiredProperties, mtaId)), version, cloudTargets);
    }

    private List<ConfigurationEntry> filter(List<ConfigurationEntry> entries, String version, List<CloudTarget> cloudTargets) {
        return entries.stream()
            .filter(entry -> VERSION_FILTER.apply(entry, version))
            .filter(entry -> VISIBILITY_FILTER.apply(entry, cloudTargets))
            .collect(Collectors.toList());
    }

    public List<ConfigurationEntry> find(String nid, String id, String version, CloudTarget target, Map<String, Object> requiredProperties,
        String mtaId) {
        return find(nid, id, version, target, requiredProperties, mtaId, null);
    }

    public List<ConfigurationEntry> find(String spaceGuid) {
        return fromDtos(dao.find(spaceGuid));
    }

    @Override
    public ConfigurationEntry find(Long id) {
        return fromDto(dao.findRequired(id));
    }

    @Override
    protected AbstractDtoDao<ConfigurationEntryDto, Long> getDtoDao() {
        return dao;
    }

    @Override
    protected ConfigurationEntry fromDto(ConfigurationEntryDto configurationEntryDto) {
        return configurationEntryDto != null ? configurationEntryDto.toConfigurationEntry() : null;
    }

    @Override
    protected ConfigurationEntryDto toDto(ConfigurationEntry configurationEntry) {
        return new ConfigurationEntryDto(configurationEntry);
    }

}
