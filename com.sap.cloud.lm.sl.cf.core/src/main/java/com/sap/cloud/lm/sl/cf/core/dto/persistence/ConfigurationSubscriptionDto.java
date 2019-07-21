package com.sap.cloud.lm.sl.cf.core.dto.persistence;

import static java.text.MessageFormat.format;

import java.util.Map;
import java.util.TreeMap;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription.ModuleDto;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription.ResourceDto;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.SequenceNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableColumnNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableNames;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Entity
@Table(name = TableNames.CONFIGURATION_SUBSCRIPTION_TABLE, uniqueConstraints = { @UniqueConstraint(columnNames = {
    TableColumnNames.CONFIGURATION_SUBSCRIPTION_APP_NAME, TableColumnNames.CONFIGURATION_SUBSCRIPTION_MTA_ID,
    TableColumnNames.CONFIGURATION_SUBSCRIPTION_SPACE_ID, TableColumnNames.CONFIGURATION_SUBSCRIPTION_RESOURCE_NAME }) })
@Access(AccessType.FIELD)
@SequenceGenerator(name = SequenceNames.CONFIGURATION_SUBSCRIPTION_SEQUENCE, sequenceName = SequenceNames.CONFIGURATION_SUBSCRIPTION_SEQUENCE, initialValue = 1, allocationSize = 1)
@NamedQuery(name = PersistenceMetadata.NamedQueries.FIND_ALL_SUBSCRIPTIONS, query = "SELECT cs FROM ConfigurationSubscriptionDto cs")
@NamedQuery(name = PersistenceMetadata.NamedQueries.FIND_ALL_SUBSCRIPTIONS_BY_SPACE_ID, query = "SELECT cs FROM ConfigurationSubscriptionDto cs WHERE cs.spaceId = :spaceId")
public class ConfigurationSubscriptionDto implements DtoWithPrimaryKey<Long> {

    public static class FieldNames {

        public static final String ID = "id";
        public static final String MTA_ID = "mtaId";
        public static final String SPACE_ID = "spaceId";
        public static final String APP_NAME = "appName";
        public static final String FILTER = "filter";
        public static final String MODULE = "module";
        public static final String RESOURCE_NAME = "resourceName";
        public static final String RESOURCE_PROP = "resourceProperties";

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

    public ConfigurationSubscriptionDto() {
        // Required by JPA.
    }

    public ConfigurationSubscriptionDto(long id, String mtaId, String spaceId, String appName, String filter, String moduleContent,
        String resourceName, String resourceProperties) {
        this.id = id;
        this.resourceProperties = resourceProperties;
        this.resourceName = resourceName;
        this.module = moduleContent;
        this.filter = filter;
        this.spaceId = spaceId;
        this.appName = appName;
        this.mtaId = mtaId;

        validateState();
    }

    public ConfigurationSubscriptionDto(ConfigurationSubscription subscription) {
        if (subscription.getFilter() != null) {
            this.filter = JsonUtil.toJson(subscription.getFilter(), false);
        }

        ResourceDto resourceDto = subscription.getResourceDto();
        ModuleDto moduleDto = subscription.getModuleDto();

        if (resourceDto != null) {
            this.resourceProperties = JsonUtil.toJson(resourceDto.getProperties(), false);
            this.resourceName = resourceDto.getName();
        }

        if (moduleDto != null) {
            this.module = JsonUtil.toJson(moduleDto, false);
        }

        this.appName = subscription.getAppName();
        this.spaceId = subscription.getSpaceId();
        this.mtaId = subscription.getMtaId();
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

    public ConfigurationSubscription toConfigurationSubscription() {
        try {
            ConfigurationFilter parsedFilter = JsonUtil.fromJson(filter, ConfigurationFilter.class);
            Map<String, Object> parsedResourceProperties = JsonUtil.convertJsonToMap(resourceProperties);
            ResourceDto resourceDto = new ResourceDto(resourceName, parsedResourceProperties);
            ModuleDto moduleDto = JsonUtil.fromJson(module, ModuleDto.class);
            return new ConfigurationSubscription(id, mtaId, spaceId, appName, parsedFilter, moduleDto, resourceDto);
        } catch (SLException e) {
            throw new IllegalStateException(format(Messages.UNABLE_TO_PARSE_SUBSCRIPTION, e.getMessage()), e);
        }
    }

}
