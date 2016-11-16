package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.ConfigurationEntryDto;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.mta.model.Version;

@Component
public class ConfigurationEntryDao {

    @Autowired
    ConfigurationEntryDtoDao dao;

    public List<ConfigurationEntry> find(String nid, String id, String version, String target, Map<String, String> requiredProperties,
        String mtaId) {
        return filter(toConfigurationEntries(dao.find(nid, id, target, requiredProperties, mtaId)), version);
    }

    private List<ConfigurationEntry> filter(List<ConfigurationEntry> entries, String version) {
        return entries.stream().filter((entry) -> VERSION_FILTER.apply(entry, version)).collect(Collectors.toList());
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

    public void remove(long id) throws NotFoundException {
        dao.remove(id);
    }

    public ConfigurationEntry add(ConfigurationEntry entry) throws ConflictException {
        return dao.add(new ConfigurationEntryDto(entry)).toConfigurationEntry();
    }

    public boolean exists(long id) {
        return dao.exists(id);
    }

    private static final BiFunction<ConfigurationEntry, String, Boolean> VERSION_FILTER = new BiFunction<ConfigurationEntry, String, Boolean>() {

        @Override
        public Boolean apply(ConfigurationEntry entry, String requirement) {
            if (requirement == null) {
                return true;
            }
            Version providerVersion = entry.getProviderVersion();
            if (providerVersion == null) {
                return false;
            }
            return providerVersion.satisfies(requirement);
        }

    };

    public List<ConfigurationEntry> findAll() {
        return toConfigurationEntries(dao.findAll());
    }

}
