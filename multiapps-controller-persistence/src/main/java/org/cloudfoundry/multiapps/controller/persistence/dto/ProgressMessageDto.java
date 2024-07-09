package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.SequenceNames;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.TableColumnNames;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.TableNames;

@Entity
@Table(name = TableNames.PROGRESS_MESSAGE_TABLE)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@SequenceGenerator(name = SequenceNames.PROGRESS_MESSAGE_SEQUENCE, sequenceName = SequenceNames.PROGRESS_MESSAGE_SEQUENCE, allocationSize = 1)
public class ProgressMessageDto implements DtoWithPrimaryKey<Long> {

    public static class AttributeNames {

        private AttributeNames() {
        }

        public static final String ID = "id";
        public static final String PROCESS_ID = "processId";
        public static final String TASK_ID = "taskId";
        public static final String TYPE = "type";
        public static final String TEXT = "text";
        public static final String TIMESTAMP = "timestamp";

    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SequenceNames.PROGRESS_MESSAGE_SEQUENCE)
    @Column(name = TableColumnNames.PROGRESS_MESSAGE_ID)
    protected long id;

    @Column(name = TableColumnNames.PROGRESS_MESSAGE_PROCESS_ID, nullable = false)
    protected String processId;

    @Column(name = TableColumnNames.PROGRESS_MESSAGE_TASK_ID, nullable = false)
    protected String taskId;

    @Column(name = TableColumnNames.PROGRESS_MESSAGE_TYPE, nullable = false)
    protected String type;

    @Convert(converter = TextAttributeConverter.class)
    @Column(name = TableColumnNames.PROGRESS_MESSAGE_TEXT, nullable = false)
    protected String text;

    @Column(name = TableColumnNames.PROGRESS_MESSAGE_TIMESTAMP)
    @Temporal(TemporalType.TIMESTAMP)
    protected Date timestamp;

    protected ProgressMessageDto() {
        // Required by JPA
    }

    public ProgressMessageDto(long id, String processId, String taskId, String type, String text, Date timestamp) {
        this.id = id;
        this.processId = processId;
        this.taskId = taskId;
        this.type = type;
        this.text = text;
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

    public String getTaskId() {
        return taskId;
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

}