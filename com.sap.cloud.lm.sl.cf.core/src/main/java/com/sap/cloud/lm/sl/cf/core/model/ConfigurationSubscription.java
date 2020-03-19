package com.sap.cloud.lm.sl.cf.core.model;

import static java.text.MessageFormat.format;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.mta.Messages;
import com.sap.cloud.lm.sl.mta.model.AuditableConfiguration;
import com.sap.cloud.lm.sl.mta.model.ConfigurationIdentifier;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class ConfigurationSubscription implements AuditableConfiguration {

    private long id;
    private String mtaId;
    private ConfigurationFilter filter;
    private String spaceId;
    private String appName;
    private ModuleDto moduleDto;
    private ResourceDto resourceDto;

    public ConfigurationSubscription() {
        // Required by Jackson
    }

    public ConfigurationSubscription(long id, String mtaId, String spaceId, String appName, ConfigurationFilter filter, ModuleDto moduleDto,
                                     ResourceDto resourceDto) {
        this.filter = filter;
        this.spaceId = spaceId;
        this.appName = appName;
        this.id = id;
        this.moduleDto = moduleDto;
        this.mtaId = mtaId;
        this.resourceDto = resourceDto;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ConfigurationFilter getFilter() {
        return filter;
    }

    public String getMtaId() {
        return mtaId;
    }

    public String getAppName() {
        return appName;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public ModuleDto getModuleDto() {
        return moduleDto;
    }

    public ResourceDto getResourceDto() {
        return resourceDto;
    }

    public static ConfigurationSubscription from(String mtaId, String spaceId, String appName, ConfigurationFilter filter, Module module,
                                                 Resource resource, int majorSchemaVersion) {
        if (majorSchemaVersion == 2) {
            ResourceDto resourceDto = ResourceDto.from2(resource);
            ModuleDto moduleDto = ModuleDto.from2(module);
            return new ConfigurationSubscription(0, mtaId, spaceId, appName, filter, moduleDto, resourceDto);
        }
        throw new UnsupportedOperationException(format(Messages.UNSUPPORTED_VERSION, majorSchemaVersion));
    }

    public static class ModuleDto {

        private String name;
        private Map<String, Object> properties;
        private List<ProvidedDependencyDto> providedDependencies;
        private List<RequiredDependencyDto> requiredDependencies;

        public ModuleDto() {

        }

        public ModuleDto(String name, Map<String, Object> properties, List<ProvidedDependencyDto> providedDependencies,
                         List<RequiredDependencyDto> requiredDependencies) {
            this.name = name;
            this.properties = properties;
            this.providedDependencies = providedDependencies;
            this.requiredDependencies = requiredDependencies;
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public List<ProvidedDependencyDto> getProvidedDependencies() {
            return providedDependencies;
        }

        public List<RequiredDependencyDto> getRequiredDependencies() {
            return requiredDependencies;
        }

        public static ModuleDto from2(Module module) {
            return new ModuleDto(module.getName(),
                                 module.getProperties(),
                                 fromProvidedDependencies2(module.getProvidedDependencies()),
                                 fromRequiredDependencies2(module.getRequiredDependencies()));
        }

        private static List<ProvidedDependencyDto> fromProvidedDependencies2(List<ProvidedDependency> providedDependencies) {
            return providedDependencies.stream()
                                       .map(ProvidedDependencyDto::from2)
                                       .collect(Collectors.toList());
        }

        private static List<RequiredDependencyDto> fromRequiredDependencies2(List<RequiredDependency> requiredDependencies) {
            return requiredDependencies.stream()
                                       .map(RequiredDependencyDto::from2)
                                       .collect(Collectors.toList());
        }

    }

    public static class ResourceDto {

        private String name;
        private Map<String, Object> properties;

        public ResourceDto() {

        }

        public ResourceDto(String name, Map<String, Object> properties) {
            this.name = name;
            this.properties = properties;
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public static ResourceDto from2(Resource resource) {
            return new ResourceDto(resource.getName(), resource.getProperties());
        }

    }

    public static class RequiredDependencyDto {

        private String name;
        private String list;
        private Map<String, Object> properties;

        public RequiredDependencyDto() {

        }

        public RequiredDependencyDto(String name, String list, Map<String, Object> properties) {
            this.name = name;
            this.list = list;
            this.properties = properties;
        }

        public String getName() {
            return name;
        }

        public String getList() {
            return list;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public static RequiredDependencyDto from2(RequiredDependency requiredDependency) {
            return new RequiredDependencyDto(requiredDependency.getName(),
                                             requiredDependency.getList(),
                                             requiredDependency.getProperties());
        }

    }

    public static class ProvidedDependencyDto {

        private String name;
        private Map<String, Object> properties;

        public ProvidedDependencyDto() {

        }

        public ProvidedDependencyDto(String name, Map<String, Object> properties) {
            this.name = name;
            this.properties = properties;
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public static ProvidedDependencyDto from2(ProvidedDependency providedDependency) {
            return new ProvidedDependencyDto(providedDependency.getName(), providedDependency.getProperties());
        }

    }

    public boolean matches(List<ConfigurationEntry> entries) {
        return entries.stream()
                      .anyMatch(entry -> filter.matches(entry));
    }

    public boolean matches(ConfigurationEntry entry) {
        return filter.matches(entry);
    }

    @Override
    public String getConfigurationType() {
        return "configuration subscription";
    }

    @Override
    public String getConfigurationName() {
        return String.valueOf(id);
    }

    @Override
    public List<ConfigurationIdentifier> getConfigurationIdentifiers() {
        return Arrays.asList(new ConfigurationIdentifier("mta id", mtaId), new ConfigurationIdentifier("application name", appName),
                             new ConfigurationIdentifier("space id", spaceId));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ConfigurationSubscription that = (ConfigurationSubscription) o;
        return id == that.id && Objects.equals(mtaId, that.mtaId) && Objects.equals(filter, that.filter)
            && Objects.equals(spaceId, that.spaceId) && Objects.equals(appName, that.appName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mtaId, filter, spaceId, appName);
    }
}
