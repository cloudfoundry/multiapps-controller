package com.sap.cloud.lm.sl.cf.core.persistence.dto;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.SequenceNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableNames;

@Entity
@Table(name = TableNames.STEP_ANALYTICS_DATA_TABLE)
@Cacheable(false)
@SequenceGenerator(name = SequenceNames.STEP_ANALYTICS_DATA_SEQUENCE, sequenceName = SequenceNames.STEP_ANALYTICS_DATA_SEQUENCE, initialValue = 1, allocationSize = 1)
public class StepAnalyticsDataDto implements DtoWithPrimaryKey<Long> {

    public static class AttributeNames {

        private AttributeNames() {
        }

        public static final String ID = "id";
        public static final String PROCESS_ID = "processId";
        public static final String PROCESS_TYPE = "processType";
        public static final String EVENT = "event";
        public static final String EVENT_OCCURRENCE_TIME = "eventOccurrenceTime";

    }

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SequenceNames.STEP_ANALYTICS_DATA_SEQUENCE)
    private long id;

    @Column(name = "process_id", nullable = false)
    private String processId;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(name = "event", nullable = false)
    private String event;

    @Column(name = "event_occurrence_time", nullable = false)
    private long eventOccurrenceTime;

    protected StepAnalyticsDataDto() {
        // Required by JPA
    }

    public StepAnalyticsDataDto(long id, String processId, String taskId, String event, long eventOccurrenceTime) {
        this.id = id;
        this.processId = processId;
        this.taskId = taskId;
        this.event = event;
        this.eventOccurrenceTime = eventOccurrenceTime;
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

    public String getEvent() {
        return event;
    }

    public Long getEventOccurrenceTime() {
        return eventOccurrenceTime;
    }

}
