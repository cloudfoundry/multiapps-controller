package com.sap.cloud.lm.sl.cf.core.dto.persistence;

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

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableHistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.SequenceNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableColumnNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableNames;

@Entity
@Table(name = TableNames.HISTORIC_OPERATION_EVENT_TABLE)
@NamedQuery(name = PersistenceMetadata.NamedQueries.FIND_ALL_HISTORIC_OPERATION_EVENTS, query = "SELECT o FROM HistoricOperationEventDto o")
@NamedQuery(name = PersistenceMetadata.NamedQueries.FIND_HISTORIC_OPERATION_EVENTS_BY_PROCESS_ID, query = "SELECT o FROM HistoricOperationEventDto o WHERE o.processId = :processId ORDER BY o.id")
@NamedQuery(name = PersistenceMetadata.NamedQueries.DELETE_HISTORIC_OPERATION_EVENTS_BY_PROCESS_ID, query = "DELETE FROM HistoricOperationEventDto o WHERE o.processId = :processId")
@NamedQuery(name = PersistenceMetadata.NamedQueries.DELETE_HISTORIC_OPERATION_EVENTS_OLDER_THAN, query = "DELETE FROM HistoricOperationEventDto o WHERE o.timestamp < :timestamp")
@SequenceGenerator(name = SequenceNames.HISTORIC_OPERATION_EVENT_SEQUENCE, sequenceName = SequenceNames.HISTORIC_OPERATION_EVENT_SEQUENCE, initialValue = 1, allocationSize = 1)
public class HistoricOperationEventDto implements DtoWithPrimaryKey<Long> {

    public static class FieldNames {

        public static final String ID = "id";
        public static final String PROCESS_ID = "processId";
        public static final String TYPE = "type";
        public static final String TIMESTAMP = "timestamp";

    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SequenceNames.HISTORIC_OPERATION_EVENT_SEQUENCE)
    @Column(name = TableColumnNames.HISTORIC_OPERATION_EVENT_ID)
    private long id;

    @Column(name = TableColumnNames.HISTORIC_OPERATION_EVENT_PROCESS_ID, nullable = false)
    private String processId;

    @Column(name = TableColumnNames.HISTORIC_OPERATION_EVENT_TYPE, nullable = false)
    private String type;

    @Column(name = TableColumnNames.HISTORIC_OPERATION_EVENT_TIMESTAMP, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    protected HistoricOperationEventDto() {
    }

    public HistoricOperationEventDto(long id, String processId, String type, Date timestamp) {
        this.id = id;
        this.processId = processId;
        this.type = type;
        this.timestamp = timestamp;
    }

    public HistoricOperationEventDto(HistoricOperationEvent historicOperatonEvent) {
        this.id = historicOperatonEvent.getId();
        this.processId = historicOperatonEvent.getProcessId();
        this.type = historicOperatonEvent.getType()
            .name();
        this.timestamp = historicOperatonEvent.getTimestamp();
    }

    @Override
    public Long getPrimaryKey() {
        return id;
    }

    public String getProcessId() {
        return processId;
    }

    public String getType() {
        return type;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    private EventType getType(String type) {
        return EventType.valueOf(type);
    }

    public HistoricOperationEvent toHistoricOperationEvent() {
        return ImmutableHistoricOperationEvent.builder()
            .id(id)
            .processId(processId)
            .type(getType(type))
            .timestamp(timestamp)
            .build();
    }

}
