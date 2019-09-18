package com.sap.cloud.lm.sl.cf.core.persistence.dto;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.SequenceNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableColumnNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableNames;

@Entity
@Table(name = TableNames.PROGRESS_MESSAGE_TABLE)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@SequenceGenerator(name = SequenceNames.PROGRESS_MESSAGE_SEQUENCE, sequenceName = SequenceNames.PROGRESS_MESSAGE_SEQUENCE, initialValue = 1, allocationSize = 1)
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