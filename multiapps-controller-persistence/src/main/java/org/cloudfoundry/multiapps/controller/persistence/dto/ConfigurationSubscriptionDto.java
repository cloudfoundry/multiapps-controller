package org.cloudfoundry.multiapps.controller.persistence.dto;

import static java.text.MessageFormat.format;

import java.util.Map;
import java.util.TreeMap;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;

import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.SequenceNames;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.TableColumnNames;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.TableNames;

@Table(name = TableNames.CONFIGURATION_SUBSCRIPTION_TABLE, uniqueConstraints = { @UniqueConstraint(columnNames = {
    TableColumnNames.CONFIGURATION_SUBSCRIPTION_APP_NAME, TableColumnNames.CONFIGURATION_SUBSCRIPTION_MTA_ID,
    TableColumnNames.CONFIGURATION_SUBSCRIPTION_SPACE_ID, TableColumnNames.CONFIGURATION_SUBSCRIPTION_RESOURCE_NAME }) })
@Entity
@Access(AccessType.FIELD)
@SequenceGenerator(name = SequenceNames.CONFIGURATION_SUBSCRIPTION_SEQUENCE, sequenceName = SequenceNames.CONFIGURATION_SUBSCRIPTION_SEQUENCE, allocationSize = 1)
public class ConfigurationSubscriptionDto implements DtoWithPrimaryKey<Long> {

    public static class AttributeNames {

        private AttributeNames() {
        }

        public static final String ID = "id";
        public static final String MTA_ID = "mtaId";
        public static final String SPACE_ID = "spaceId";
        public static final String APP_NAME = "appName";
        public static final String FILTER = "filter";
        public static final String MODULE = "module";
        public static final String RESOURCE_NAME = "resourceName";
        public static final String RESOURCE_PROP = "resourceProperties";
        public static final String MODULE_ID = "moduleId";
        public static final String RESOURCE_ID = "resourceId";

    }

    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SequenceNames.CONFIGURATION_SUBSCRIPTION_SEQUENCE)
    @Id
    @Column(name = TableColumnNames.CONFIGURATION_SUBSCRIPTION_ID, nullable = false)
    private long id;

    @Column(name = TableColumnNames.CONFIGURATION_SUBSCRIPTION_MTA_ID, nullable = false)
    private String mtaId;

    @Column(name = TableColumnNames.CONFIGURATION_SUBSCRIPTION_SPACE_ID, nullable = false)
    private String spaceId;

    @Column(name = TableColumnNames.CONFIGURATION_SUBSCRIPTION_APP_NAME, nullable = false)
    private String appName;

    @Column(name = TableColumnNames.CONFIGURATION_SUBSCRIPTION_FILTER, nullable = false)
    @Lob
    private String filter;

    @Column(name = TableColumnNames.CONFIGURATION_SUBSCRIPTION_RESOURCE_PROP, nullable = false)
    @Lob
    private String resourceProperties;

    @Column(name = TableColumnNames.CONFIGURATION_SUBSCRIPTION_RESOURCE_NAME, nullable = false)
    private String resourceName;

    @Column(name = TableColumnNames.CONFIGURATION_SUBSCRIPTION_MODULE, nullable = false)
    @Lob
    private String module;

    @XmlElement(name = "module-id")
    @Lob
    @Column(name = TableColumnNames.CONFIGURATION_SUBSCRIPTION_MODULE_ID)
    private String moduleId;

    @XmlElement(name = "resource-id")
    @Lob
    @Column(name = TableColumnNames.CONFIGURATION_SUBSCRIPTION_RESOURCE_ID)
    private String resourceId;

    protected ConfigurationSubscriptionDto() {
        // Required by JPA.
    }

    private ConfigurationSubscriptionDto(long id, String mtaId, String spaceId, String appName, String filter, String moduleContent,
                                         String resourceName, String resourceProperties, String moduleId, String resourceId) {
        this.id = id;
        this.resourceProperties = resourceProperties;
        this.resourceName = resourceName;
        this.module = moduleContent;
        this.filter = filter;
        this.spaceId = spaceId;
        this.appName = appName;
        this.mtaId = mtaId;
        this.moduleId = moduleId;
        this.resourceId = resourceId;

        validateState();
    }

    private void validateState() {
        for (Map.Entry<String, String> field : getFields().entrySet()) {
            if (field.getValue() == null) {
                throw new IllegalStateException(format(Messages.COLUMN_VALUE_SHOULD_NOT_BE_NULL, field.getKey()));
            }
        }
    }

    private Map<String, String> getFields() {
        Map<String, String> fields = new TreeMap<>();
        fields.put(TableColumnNames.CONFIGURATION_SUBSCRIPTION_MTA_ID, mtaId);
        fields.put(TableColumnNames.CONFIGURATION_SUBSCRIPTION_APP_NAME, appName);
        fields.put(TableColumnNames.CONFIGURATION_SUBSCRIPTION_SPACE_ID, spaceId);
        fields.put(TableColumnNames.CONFIGURATION_SUBSCRIPTION_FILTER, filter);
        fields.put(TableColumnNames.CONFIGURATION_SUBSCRIPTION_MODULE, module);
        fields.put(TableColumnNames.CONFIGURATION_SUBSCRIPTION_RESOURCE_PROP, resourceProperties);
        fields.put(TableColumnNames.CONFIGURATION_SUBSCRIPTION_RESOURCE_NAME, resourceName);
        return fields;
    }

    @Override
    public Long getPrimaryKey() {
        return id;
    }

    @Override
    public void setPrimaryKey(Long id) {
        this.id = id;
    }

    public String getMtaId() {
        return mtaId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getAppName() {
        return appName;
    }

    public String getFilter() {
        return filter;
    }

    public String getModuleContent() {
        return module;
    }

    public String getResourceProperties() {
        return resourceProperties;
    }

    public String getResourceName() {
        return resourceName;
    }
    
    public String getModuleId() {
        return moduleId;
    }
    
    public String getResourceId() {
        return resourceId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private long id;
        private String mtaId;
        private String spaceId;
        private String appName;
        private String filter;
        private String resourceProperties;
        private String resourceName;
        private String module;
        private String moduleId;
        private String resourceId;

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder mtaId(String mtaId) {
            this.mtaId = mtaId;
            return this;
        }

        public Builder spaceId(String spaceId) {
            this.spaceId = spaceId;
            return this;
        }

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder filter(String filter) {
            this.filter = filter;
            return this;
        }

        public Builder resourceProperties(String resourceProperties) {
            this.resourceProperties = resourceProperties;
            return this;
        }

        public Builder resourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder moduleId(String moduleId) {
            this.moduleId = moduleId;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public ConfigurationSubscriptionDto build() {
            return new ConfigurationSubscriptionDto(id,
                                                    mtaId,
                                                    spaceId,
                                                    appName,
                                                    filter,
                                                    module,
                                                    resourceName,
                                                    resourceProperties,
                                                    moduleId,
                                                    resourceId);
        }
    }
}
