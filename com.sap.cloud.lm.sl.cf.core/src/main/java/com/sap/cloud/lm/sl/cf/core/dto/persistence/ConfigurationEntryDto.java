package com.sap.cloud.lm.sl.cf.core.dto.persistence;

import java.util.List;

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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.SequenceNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableColumnNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableNames;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.Version;

@Entity
@Access(AccessType.FIELD)
@Table(name = TableNames.CONFIGURATION_ENTRY_TABLE, uniqueConstraints = {
    @UniqueConstraint(columnNames = { TableColumnNames.CONFIGURATION_ENTRY_PROVIDER_NID, TableColumnNames.CONFIGURATION_ENTRY_PROVIDER_ID,
        TableColumnNames.CONFIGURATION_ENTRY_PROVIDER_VERSION, TableColumnNames.CONFIGURATION_ENTRY_TARGET_SPACE }) })
@NamedQuery(name = PersistenceMetadata.NamedQueries.FIND_ALL_ENTRIES, query = "SELECT ce FROM ConfigurationEntryDto ce")
@NamedQuery(name = PersistenceMetadata.NamedQueries.FIND_ALL_ENTRIES_BY_SPACE_ID, query = "SELECT ce FROM ConfigurationEntryDto ce WHERE ce.spaceId = :spaceId")
@SequenceGenerator(name = SequenceNames.CONFIGURATION_ENTRY_SEQUENCE, sequenceName = SequenceNames.CONFIGURATION_ENTRY_SEQUENCE, initialValue = 1, allocationSize = 1)
@XmlRootElement(name = "configuration-entry")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class ConfigurationEntryDto implements DtoWithPrimaryKey<Long> {

    public static class FieldNames {

        public static final String ID = "id";
        public static final String PROVIDER_ID = "providerId";
        public static final String PROVIDER_VERSION = "providerVersion";
        public static final String PROVIDER_NID = "providerNid";
        public static final String TARGET_ORG = "targetOrg";
        public static final String TARGET_SPACE = "targetSpace";
        public static final String SPACE_ID = "spaceId";
        public static final String CONTENT = "content";
        public static final String VISIBILITY = "visibility";
    }

    @XmlElement
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SequenceNames.CONFIGURATION_ENTRY_SEQUENCE)
    @Column(name = TableColumnNames.CONFIGURATION_ENTRY_ID)
    private long id;

    @XmlElement(name = "provider-nid")
    @Column(name = TableColumnNames.CONFIGURATION_ENTRY_PROVIDER_NID, nullable = false)
    private String providerNid;

    @XmlElement(name = "provider-id")
    @Column(name = TableColumnNames.CONFIGURATION_ENTRY_PROVIDER_ID, nullable = false)
    private String providerId;

    @XmlElement(name = "provider-version")
    @Column(name = TableColumnNames.CONFIGURATION_ENTRY_PROVIDER_VERSION, nullable = false)
    private String providerVersion;

    @XmlElement(name = "target-org")
    @Column(name = TableColumnNames.CONFIGURATION_ENTRY_TARGET_ORG, nullable = false)
    private String targetOrg;

    @XmlElement(name = "target-space")
    @Column(name = TableColumnNames.CONFIGURATION_ENTRY_TARGET_SPACE, nullable = false)
    private String targetSpace;

    @XmlElement(name = "space-id")
    @Column(name = TableColumnNames.CONFIGURATION_ENTRY_SPACE_ID, nullable = false)
    private String spaceId;

    @XmlElement
    @Lob
    @Column(name = TableColumnNames.CONFIGURATION_ENTRY_CONTENT)
    private String content;

    @XmlElement(name = "visibility")
    @Lob
    @Column(name = TableColumnNames.CONFIGURATION_CLOUD_TARGET, nullable = true)
    private String visibility;

    public ConfigurationEntryDto() {
        // Required by JPA and JAXB.
    }

    public ConfigurationEntryDto(long id, String providerNid, String providerId, String providerVersion, String targetOrg,
        String targetSpace, String content, String visibility, String spaceId) {
        this.id = id;
        this.providerNid = providerNid;
        this.providerId = providerId;
        this.providerVersion = providerVersion;
        this.targetOrg = targetOrg;
        this.targetSpace = targetSpace;
        this.content = content;
        this.visibility = visibility;
        this.spaceId = spaceId;
    }

    public ConfigurationEntryDto(ConfigurationEntry entry) {
        this.id = entry.getId();
        this.providerNid = getNotNull(entry.getProviderNid());
        this.providerId = entry.getProviderId();
        this.providerVersion = getNotNull(entry.getProviderVersion());
        this.targetSpace = entry.getTargetSpace() == null ? null
            : entry.getTargetSpace()
                .getSpace();
        this.targetOrg = entry.getTargetSpace() == null ? null
            : entry.getTargetSpace()
                .getOrg();
        this.content = entry.getContent();
        this.visibility = entry.getVisibility() == null ? null : JsonUtil.toJson(entry.getVisibility());
        this.spaceId = entry.getSpaceId();
    }

    @Override
    public Long getPrimaryKey() {
        return id;
    }

    public String getProviderNid() {
        return providerNid;
    }

    public String getTargetSpace() {
        return targetSpace;
    }

    public String getTargetOrg() {
        return targetOrg;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getProviderVersion() {
        return providerVersion;
    }

    public String getContent() {
        return content;
    }

    public String getVisibility() {
        return visibility;
    }

    public ConfigurationEntry toConfigurationEntry() {
        return new ConfigurationEntry(id, getOriginal(providerNid), providerId, getParsedVersion(getOriginal(providerVersion)),
            new CloudTarget(targetOrg, targetSpace), content, getParsedVisibility(visibility), spaceId);
    }

    private Version getParsedVersion(String versionString) {
        if (versionString == null) {
            return null;
        }
        return Version.parseVersion(versionString);
    }

    private List<CloudTarget> getParsedVisibility(String visibility) {
        if (visibility == null) {
            return null;
        }
        return JsonUtil.convertJsonToList(visibility, new TypeReference<List<CloudTarget>>() {
        });
    }

    private String getOriginal(String source) {
        if (source == null || source.equals(PersistenceMetadata.NOT_AVAILABLE)) {
            return null;
        }
        return source;
    }

    private String getNotNull(Object source) {
        if (source == null) {
            return PersistenceMetadata.NOT_AVAILABLE;
        }
        return source.toString();
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

}
