package com.sap.cloud.lm.sl.cf.core.dto;

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

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.NamedQueries;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.SequenceNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableColumnNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableNames;
import com.sap.cloud.lm.sl.mta.model.Version;

@Entity
@Access(AccessType.FIELD)
@Table(name = TableNames.CONFIGURATION_ENTRY_TABLE, uniqueConstraints = {
    @UniqueConstraint(columnNames = { TableColumnNames.CONFIGURATION_ENTRY_PROVIDER_NID, TableColumnNames.CONFIGURATION_ENTRY_PROVIDER_ID,
        TableColumnNames.CONFIGURATION_ENTRY_PROVIDER_VERSION, TableColumnNames.CONFIGURATION_ENTRY_TARGET_SPACE }) })
@NamedQuery(name = NamedQueries.FIND_ALL_ENTRIES, query = "SELECT ce FROM ConfigurationEntryDto ce")
@SequenceGenerator(name = SequenceNames.CONFIGURATION_ENTRY_SEQUENCE, sequenceName = SequenceNames.CONFIGURATION_ENTRY_SEQUENCE, initialValue = 1, allocationSize = 1)
@XmlRootElement(name = "configuration-entry")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class ConfigurationEntryDto {

    public static class FieldNames {

        public static final String ID = "id";
        public static final String PROVIDER_ID = "providerId";
        public static final String PROVIDER_VERSION = "providerVersion";
        public static final String PROVIDER_NID = "providerNid";
        public static final String TARGET_SPACE = "targetSpace";
        public static final String CONTENT = "content";

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

    @XmlElement(name = "target-space")
    @Column(name = TableColumnNames.CONFIGURATION_ENTRY_TARGET_SPACE, nullable = false)
    private String targetSpace;

    @XmlElement
    @Lob
    @Column(name = TableColumnNames.CONFIGURATION_ENTRY_CONTENT)
    private String content;

    public ConfigurationEntryDto() {
        // Required by JPA and JAXB.
    }

    public ConfigurationEntryDto(long id, String providerNid, String providerId, String providerVersion, String targetSpace,
        String content) {
        this.id = id;
        this.providerNid = providerNid;
        this.providerId = providerId;
        this.providerVersion = providerVersion;
        this.targetSpace = targetSpace;
        this.content = content;
    }

    public ConfigurationEntryDto(ConfigurationEntry entry) {
        this.id = entry.getId();
        this.providerNid = getNotNull(entry.getProviderNid());
        this.providerId = entry.getProviderId();
        this.providerVersion = getNotNull(entry.getProviderVersion());
        this.targetSpace = entry.getTargetSpace();
        this.content = entry.getContent();
    }

    public long getId() {
        return id;
    }

    public String getProviderNid() {
        return providerNid;
    }

    public String getTargetSpace() {
        return targetSpace;
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

    public ConfigurationEntry toConfigurationEntry() {
        return new ConfigurationEntry(id, getOriginal(providerNid), providerId, getParsedVersion(getOriginal(providerVersion)), targetSpace,
            content);
    }

    private Version getParsedVersion(String versionString) {
        if (versionString == null) {
            return null;
        }
        return Version.parseVersion(versionString);
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

}
