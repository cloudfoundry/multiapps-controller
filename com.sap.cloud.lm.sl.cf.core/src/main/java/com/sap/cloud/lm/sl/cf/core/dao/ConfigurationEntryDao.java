package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.ConfigurationEntryDto;
import com.sap.cloud.lm.sl.cf.core.filters.VersionFilter;
import com.sap.cloud.lm.sl.cf.core.filters.VisibilityFilter;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;

@Component
public class ConfigurationEntryDao {

    @Autowired
    ConfigurationEntryDtoDao dao;

    private static final BiFunction<ConfigurationEntry, String, Boolean> VERSION_FILTER = new VersionFilter();
    private static final BiFunction<ConfigurationEntry, List<CloudTarget>, Boolean> VISIBILITY_FILTER = new VisibilityFilter();

    public List<ConfigurationEntry> find(String nid, String id, String version, CloudTarget target, Map<String, Object> requiredProperties,
        String mtaId, List<CloudTarget> cloudTargets) {
        return filter(toConfigurationEntries(dao.find(nid, id, target, requiredProperties, mtaId)), version, cloudTargets);
    }

    public List<ConfigurationEntry> find(String nid, String id, String version, CloudTarget target, Map<String, Object> requiredProperties,
        String mtaId) {
        return find(nid, id, version, target, requiredProperties, mtaId, null);
    }

    private List<ConfigurationEntry> filter(List<ConfigurationEntry> entries, String version, List<CloudTarget> cloudTargets) {
        return entries.stream().filter((entry) -> VERSION_FILTER.apply(entry, version)).filter(
            (entry) -> VISIBILITY_FILTER.apply(entry, cloudTargets)).collect(Collectors.toList());
    }

    private List<ConfigurationEntry> toConfigurationEntries(List<ConfigurationEntryDto> dtos) {
        return dtos.stream().map((dto) -> dto.toConfigurationEntry()).collect(Collectors.toList());
    }

    public ConfigurationEntry update(long id, ConfigurationEntry entry) throws ConflictException, NotFoundException {
        return dao.update(id, new ConfigurationEntryDto(entry)).toConfigurationEntry();
    }

    public ConfigurationEntry find(long id) throws NotFoundException {
        return dao.find(id).toConfigurationEntry();
    }
    
    public List<ConfigurationEntry> find(String spaceGuid){
        return toConfigurationEntries(dao.find(spaceGuid));
    }

    public void remove(long id) throws NotFoundException {
        dao.remove(id);
    }
    
    public List<ConfigurationEntry> removeAll(List<ConfigurationEntry> configurationEntries){
        return toConfigurationEntries(dao.removeAll(toConfigurationEntryDtos(configurationEntries)));
    }

    public ConfigurationEntry add(ConfigurationEntry entry) throws ConflictException {
        return dao.add(new ConfigurationEntryDto(entry)).toConfigurationEntry();
    }
    
    private List<ConfigurationEntryDto> toConfigurationEntryDtos(List<ConfigurationEntry> configurationEntries){
        List<ConfigurationEntryDto> result = new ArrayList<>();
        for(ConfigurationEntry configurationEntry: configurationEntries){
            result.add(new ConfigurationEntryDto(configurationEntry));
        }
        return result;
    }

    public boolean exists(long id) {
        return dao.exists(id);
    }

    public List<ConfigurationEntry> findAll() {
        return toConfigurationEntries(dao.findAll());
    }

}
