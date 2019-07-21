package com.sap.cloud.lm.sl.cf.core.dto.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.SequenceNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableColumnNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableNames;
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;

@Entity
@Table(name = TableNames.PROGRESS_MESSAGE_TABLE)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@NamedNativeQuery(name = PersistenceMetadata.NamedQueries.FIND_ALL_PROGRESS_MESSAGES, query = "SELECT * FROM progress_message ORDER BY id", resultClass = ProgressMessageDto.class)
@NamedNativeQuery(name = PersistenceMetadata.NamedQueries.FIND_PROGRESS_MESSAGES_BY_PROCESS_ID, query = "SELECT * FROM progress_message WHERE process_id = ? ORDER BY id", resultClass = ProgressMessageDto.class)
@NamedNativeQuery(name = PersistenceMetadata.NamedQueries.DELETE_PROGRESS_MESSAGES_BY_PROCESS_ID, query = "DELETE FROM progress_message WHERE process_id = ?")
@NamedNativeQuery(name = PersistenceMetadata.NamedQueries.DELETE_PROGRESS_MESSAGES_OLDER_THAN, query = "DELETE FROM progress_message WHERE timestamp < ?")
@NamedNativeQuery(name = PersistenceMetadata.NamedQueries.DELETE_PROGRESS_MESSAGES_BY_PROCESS_AND_TASK_ID_AND_TYPE, query = "DELETE FROM progress_message WHERE process_id = ? AND task_id = ? AND type = ?")
@SequenceGenerator(name = SequenceNames.PROGRESS_MESSAGE_SEQUENCE, sequenceName = SequenceNames.PROGRESS_MESSAGE_SEQUENCE, initialValue = 1, allocationSize = 1)
public class ProgressMessageDto implements DtoWithPrimaryKey<Long> {

    public static class FieldNames {

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

    public ProgressMessageDto(ProgressMessage progressMessage) {
        this.id = progressMessage.getId();
        this.processId = progressMessage.getProcessId();
        this.taskId = progressMessage.getTaskId();
        this.type = progressMessage.getType() != null ? progressMessage.getType()
            .name() : null;
        this.text = progressMessage.getText();
        this.timestamp = progressMessage.getTimestamp();
    }

    @Override
    public Long getPrimaryKey() {
        return id;
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

    public ProgressMessage toProgressMessage() {
        return ImmutableProgressMessage.builder()
            .id(id)
            .processId(processId)
            .taskId(taskId)
            .type(getParsedType(type))
            .text(text)
            .timestamp(timestamp)
            .build();
    }

    private ProgressMessageType getParsedType(String type) {
        return type == null ? null : ProgressMessageType.valueOf(type);
    }

}
