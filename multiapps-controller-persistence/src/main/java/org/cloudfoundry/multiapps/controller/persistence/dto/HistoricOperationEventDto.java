package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.SequenceNames;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.TableColumnNames;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.TableNames;

@Entity
@Table(name = TableNames.HISTORIC_OPERATION_EVENT_TABLE)
@SequenceGenerator(name = SequenceNames.HISTORIC_OPERATION_EVENT_SEQUENCE, sequenceName = SequenceNames.HISTORIC_OPERATION_EVENT_SEQUENCE, allocationSize = 1)
public class HistoricOperationEventDto implements DtoWithPrimaryKey<Long> {

    public static class AttributeNames {

        private AttributeNames() {
        }

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
    private LocalDateTime timestamp;

    protected HistoricOperationEventDto() {
        // Required by JPA
    }

    public HistoricOperationEventDto(long id, String processId, String type, LocalDateTime timestamp) {
        this.id = id;
        this.processId = processId;
        this.type = type;
        this.timestamp = timestamp;
    }

    @Override
    public Long getPrimaryKey() {
        return id;
    }

    @Override
    public void setPrimaryKey(Long id) {
        this.id = id;
    }

    public String getProcessId() {
        return processId;
    }

    public String getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

}