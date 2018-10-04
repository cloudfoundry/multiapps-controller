package com.sap.cloud.lm.sl.cf.core.model;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.google.gson.annotations.Expose;
import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.common.model.xml.PropertiesAdapter;
import com.sap.cloud.lm.sl.mta.message.Messages;
import com.sap.cloud.lm.sl.mta.model.AuditableConfiguration;
import com.sap.cloud.lm.sl.mta.model.ConfigurationIdentifier;
import com.sap.cloud.lm.sl.mta.model.v1.Module;
import com.sap.cloud.lm.sl.mta.model.v1.Resource;
import com.sap.cloud.lm.sl.mta.model.v2.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency;

@XmlRootElement(name = "configuration-subscription")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class ConfigurationSubscription implements AuditableConfiguration {

    @XmlElement
    private long id;

    @Expose
    @XmlElement(name = "mta-id")
    private String mtaId;
    @Expose
    @XmlElement(name = "configuration-filter")
    private ConfigurationFilter filter;
    @Expose
    @XmlElement(name = "space-id")
    private String spaceId;
    @Expose
    @XmlElement(name = "app-name")
    private String appName;
    @Expose
    @XmlElement(name = "module")
    private ModuleDto moduleDto;
    @Expose
    @XmlElement(name = "resource")
    private ResourceDto resourceDto;

    public ConfigurationSubscription() {
        // Required by JaxB
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
        switch (majorSchemaVersion) {
            case 2:
                ResourceDto resourceDto = ResourceDto.from2((com.sap.cloud.lm.sl.mta.model.v2.Resource) resource);
                ModuleDto moduleDto = ModuleDto.from2((com.sap.cloud.lm.sl.mta.model.v2.Module) module);
                return new ConfigurationSubscription(00, mtaId, spaceId, appName, filter, moduleDto, resourceDto);
            default:
                throw new UnsupportedOperationException(format(Messages.UNSUPPORTED_VERSION, majorSchemaVersion));
        }
    }

    @XmlRootElement(name = "module")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ModuleDto {

        @Expose
        @XmlElement
        private String name;
        @Expose
        @XmlElement
        @XmlJavaTypeAdapter(value = PropertiesAdapter.class)
        private Map<String, Object> properties;
        @Expose
        @XmlElement(name = "provided-dependency")
        @XmlElementWrapper(name = "provided-dependencies")
        private List<ProvidedDependencyDto> providedDependencies;
        @Expose
        @XmlElement(name = "required-dependency")
        @XmlElementWrapper(name = "required-dependencies")
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

        public static ModuleDto from2(com.sap.cloud.lm.sl.mta.model.v2.Module module) {
            return new ModuleDto(module.getName(), module.getProperties(), fromProvidedDependencies2(module.getProvidedDependencies2()),
                fromRequiredDependencies2(module.getRequiredDependencies2()));
        }

        private static List<ProvidedDependencyDto> fromProvidedDependencies2(List<ProvidedDependency> providedDependencies) {
            return providedDependencies.stream()
                .map(dependency -> ProvidedDependencyDto.from2(dependency))
                .collect(Collectors.toList());
        }

        private static List<RequiredDependencyDto> fromRequiredDependencies2(List<RequiredDependency> requiredDependencies) {
            return requiredDependencies.stream()
                .map(dependency -> RequiredDependencyDto.from2(dependency))
                .collect(Collectors.toList());
        }

    }

    @XmlRootElement(name = "resource")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ResourceDto {

        @Expose
        @XmlElement
        private String name;
        @Expose
        @XmlElement
        @XmlJavaTypeAdapter(value = PropertiesAdapter.class)
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

        public static ResourceDto from2(com.sap.cloud.lm.sl.mta.model.v2.Resource resource) {
            return new ResourceDto(resource.getName(), resource.getProperties());
        }

    }

    @XmlRootElement(name = "required-dependency")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RequiredDependencyDto {

        @Expose
        @XmlElement
        private String name;
        @Expose
        @XmlElement
        private String list;
        @Expose
        @XmlElement
        @XmlJavaTypeAdapter(value = PropertiesAdapter.class)
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

        public static RequiredDependencyDto from2(com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency requiredDependency) {
            return new RequiredDependencyDto(requiredDependency.getName(), requiredDependency.getList(),
                requiredDependency.getProperties());
        }

    }

    @XmlRootElement(name = "provided-dependency")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProvidedDependencyDto {

        @Expose
        @XmlElement
        private String name;
        @Expose
        @XmlElement
        @XmlJavaTypeAdapter(value = PropertiesAdapter.class)
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

        public static ProvidedDependencyDto from2(com.sap.cloud.lm.sl.mta.model.v2.ProvidedDependency providedDependency) {
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
        List<ConfigurationIdentifier> configurationIdentifiers = new ArrayList<>();
        configurationIdentifiers.add(new ConfigurationIdentifier("mta id", mtaId));
        configurationIdentifiers.add(new ConfigurationIdentifier("application name", appName));
        configurationIdentifiers.add(new ConfigurationIdentifier("space id", spaceId));
        return configurationIdentifiers;
    }

}
