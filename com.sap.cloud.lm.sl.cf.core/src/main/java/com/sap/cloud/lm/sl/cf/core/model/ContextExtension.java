package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;

import com.google.gson.annotations.Expose;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.NamedQueries;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableColumnNames;

@Entity
@Table(name = "context_extension", uniqueConstraints = {
    @UniqueConstraint(columnNames = { TableColumnNames.CONTEXT_EXTENSION_PROCESS_ID, TableColumnNames.CONTEXT_EXTENSION_VARIABLE_NAME }) })
@SequenceGenerator(name = "context_extension_sequence", sequenceName = "context_extension_sequence", initialValue = 1, allocationSize = 1)
@javax.persistence.NamedQueries({
    @NamedQuery(name = NamedQueries.FIND_ALL_CONTEXT_EXTENSION_ENTRIES, query = "SELECT ce FROM ContextExtension ce"),
    @NamedQuery(name = NamedQueries.FIND_ALL_CONTEXT_EXTENSION_ENTRIES_BY_PROCESS_ID, query = "SELECT ce FROM ContextExtension ce WHERE ce.processId = :processId") })

public class ContextExtension {

    public class FieldNames {
        public static final String ID = "id";
        public static final String PROCESS_ID = "processId";
        public static final String NAME = "name";
        public static final String VALUE = "value";
        public static final String CREATE_TIME = "createTime";
        public static final String UPDATE_TIME = "lastUpdatedTime";
    }

    @XmlElement
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "context_extension_sequence")
    @Column(name = TableColumnNames.CONTEXT_EXTENSION_ID)
    private long id;

    @Expose
    @Column(name = TableColumnNames.CONTEXT_EXTENSION_PROCESS_ID, nullable = false)
    private String processId;

    @Expose
    @Column(name = TableColumnNames.CONTEXT_EXTENSION_VARIABLE_NAME, nullable = false)
    private String name;

    @Expose
    @Column(name = TableColumnNames.CONTEXT_EXTENSION_VARIABLE_VALUE, nullable = false)
    private String value;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = TableColumnNames.CONTEXT_EXTENSION_CREATE_TIME, nullable = false)
    private Date createTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = TableColumnNames.CONTEXT_EXTENSION_UPDATE_TIME, nullable = false)
    private Date lastUpdatedTime;

    public ContextExtension() {
        // Required by JPA
    }

    public ContextExtension(long id, String processId, String variableKey, String variableValue, Date createTime, Date updateTime) {
        this.id = id;
        this.processId = processId;
        this.name = variableKey;
        this.value = variableValue;
        this.createTime = createTime;
        this.lastUpdatedTime = updateTime;
    }

    public ContextExtension(String processId, String variableKey, String variableValue, Date createTime, Date updateTime) {
        this(0, processId, variableKey, variableValue, createTime, updateTime);
    }

    public long getId() {
        return id;
    }

    public String getProcessId() {
        return processId;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Date getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void setLastUpdatedTime(Date updateTime) {
        this.lastUpdatedTime = updateTime;
    }
}
