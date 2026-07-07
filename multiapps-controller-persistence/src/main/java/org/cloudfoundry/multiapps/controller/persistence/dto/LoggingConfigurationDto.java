package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.TableColumnNames;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.TableNames;

@Entity
@Table(name = TableNames.CLOUD_LOGGING_SERVICE_CONFIGURATION_TABLE)
public class LoggingConfigurationDto implements DtoWithPrimaryKey<String> {

    public static final class AttributeNames {

        private AttributeNames() {
        }

        public static final String MTA_ID = "mtaId";
        public static final String MTA_SPACE = "mtaSpace";
        public static final String MTA_SPACE_ID = "mtaSpaceId";
        public static final String NAMESPACE = "namespace";
    }

    @Id
    @Column(name = TableColumnNames.CLOUD_LOGGING_ID, nullable = false, unique = true)
    private String id;

    @Column(name = TableColumnNames.CLOUD_LOGGING_TARGET_SPACE, nullable = false)
    private String targetSpace;

    @Column(name = TableColumnNames.CLOUD_LOGGING_TARGET_ORG, nullable = false)
    private String targetOrg;

    @Column(name = TableColumnNames.CLOUD_LOGGING_MTA_ID, nullable = false)
    private String mtaId;

    @Column(name = TableColumnNames.CLOUD_LOGGING_MTA_ORG, nullable = false)
    private String mtaOrg;

    @Column(name = TableColumnNames.CLOUD_LOGGING_MTA_SPACE, nullable = false)
    private String mtaSpace;

    @Column(name = TableColumnNames.CLOUD_LOGGING_MTA_SPACE_ID, nullable = false)
    private String mtaSpaceId;

    @Column(name = TableColumnNames.CLOUD_LOGGING_NAMESPACE)
    private String namespace;

    @Column(name = TableColumnNames.CLOUD_LOGGING_SERVICE_INSTANCE_NAME, nullable = false)
    private String serviceInstanceName;

    @Column(name = TableColumnNames.CLOUD_LOGGING_SERVICE_KEY_NAME, nullable = false)
    private String serviceKeyName;

    @Column(name = TableColumnNames.CLOUD_LOGGING_LOG_LEVEL, nullable = false)
    private String logLevel;

    @Column(name = TableColumnNames.CLOUD_LOGGING_IS_FAILSAFE, nullable = false)
    private boolean failSafe;

    @Column(name = TableColumnNames.CLOUD_LOGGING_ADDED_AT, nullable = false)
    private LocalDateTime addedAt;

    public LoggingConfigurationDto() {
        // Required by JPA
    }

    @Override
    public String getPrimaryKey() {
        return id;
    }

    @Override
    public void setPrimaryKey(String id) {
        this.id = id;
    }

    public String getTargetSpace() {
        return targetSpace;
    }

    public void setTargetSpace(String targetSpace) {
        this.targetSpace = targetSpace;
    }

    public String getTargetOrg() {
        return targetOrg;
    }

    public void setTargetOrg(String targetOrg) {
        this.targetOrg = targetOrg;
    }

    public String getMtaId() {
        return mtaId;
    }

    public void setMtaId(String mtaId) {
        this.mtaId = mtaId;
    }

    public String getMtaOrg() {
        return mtaOrg;
    }

    public void setMtaOrg(String mtaOrg) {
        this.mtaOrg = mtaOrg;
    }

    public String getMtaSpace() {
        return mtaSpace;
    }

    public void setMtaSpace(String mtaSpace) {
        this.mtaSpace = mtaSpace;
    }

    public String getMtaSpaceId() {
        return mtaSpaceId;
    }

    public void setMtaSpaceId(String mtaSpaceId) {
        this.mtaSpaceId = mtaSpaceId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getServiceInstanceName() {
        return serviceInstanceName;
    }

    public void setServiceInstanceName(String serviceInstanceName) {
        this.serviceInstanceName = serviceInstanceName;
    }

    public String getServiceKeyName() {
        return serviceKeyName;
    }

    public void setServiceKeyName(String serviceKeyName) {
        this.serviceKeyName = serviceKeyName;
    }

    public LogLevel getLogLevel() {
        return logLevel == null ? null : LogLevel.get(logLevel);
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel == null ? null : logLevel.name();
    }

    public boolean isFailSafe() {
        return failSafe;
    }

    public void setFailSafe(boolean failSafe) {
        this.failSafe = failSafe;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
