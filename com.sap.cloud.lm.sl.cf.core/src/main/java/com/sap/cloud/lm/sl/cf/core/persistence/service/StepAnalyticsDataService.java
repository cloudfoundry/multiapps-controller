package com.sap.cloud.lm.sl.cf.core.persistence.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableStepAnalyticsData;
import com.sap.cloud.lm.sl.cf.core.model.StepAnalyticsData;
import com.sap.cloud.lm.sl.cf.core.model.StepAnalyticsData.StepEvent;
import com.sap.cloud.lm.sl.cf.core.persistence.dto.StepAnalyticsDataDto;
import com.sap.cloud.lm.sl.cf.core.persistence.query.StepAnalyticsDataQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.impl.StepAnalyticsDataQueryImpl;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;

@Named
public class StepAnalyticsDataService extends PersistenceService<StepAnalyticsData, StepAnalyticsDataDto, Long> {

    @Inject
    private StepAnalyticsDataMapper stepAnalyticsDataMapper;

    @Inject
    public StepAnalyticsDataService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public StepAnalyticsDataQuery createQuery() {
        return new StepAnalyticsDataQueryImpl(createEntityManager(), stepAnalyticsDataMapper);
    }

    @Override
    protected PersistenceObjectMapper<StepAnalyticsData, StepAnalyticsDataDto> getPersistenceObjectMapper() {
        return stepAnalyticsDataMapper;
    }

    @Override
    protected void onEntityConflict(StepAnalyticsDataDto stepAnalyticsData, Throwable t) {
        throw new ConflictException(t,
                                    Messages.STEP_ANALYTICS_DATA_ALREADY_EXISTS,
                                    stepAnalyticsData.getPrimaryKey(),
                                    stepAnalyticsData.getProcessId(),
                                    stepAnalyticsData.getTaskId());
    }

    @Override
    protected void onEntityNotFound(Long id) {
        throw new NotFoundException(Messages.STEP_ANALYTICS_DATA_NOT_FOUND, id);
    }

    @Named
    public static class StepAnalyticsDataMapper implements PersistenceObjectMapper<StepAnalyticsData, StepAnalyticsDataDto> {

        @Override
        public StepAnalyticsData fromDto(StepAnalyticsDataDto dto) {
            return ImmutableStepAnalyticsData.builder()
                                             .id(dto.getPrimaryKey())
                                             .processId(dto.getProcessId())
                                             .taskId(dto.getTaskId())
                                             .event(getEvent(dto.getEvent()))
                                             .eventOccurrenceTime(dto.getEventOccurrenceTime())
                                             .build();
        }

        private StepEvent getEvent(String event) {
            return event == null ? null : StepEvent.valueOf(event);
        }

        @Override
        public StepAnalyticsDataDto toDto(StepAnalyticsData stepAnalyticsData) {
            long id = stepAnalyticsData.getId();
            String processId = stepAnalyticsData.getProcessId();
            String taskId = stepAnalyticsData.getTaskId();
            String event = stepAnalyticsData.getEvent() != null ? stepAnalyticsData.getEvent()
                                                                                   .name()
                : null;
            long eventOccurrenceTime = stepAnalyticsData.getEventOccurrenceTime();
            return new StepAnalyticsDataDto(id, processId, taskId, event, eventOccurrenceTime);
        }

    }
}
