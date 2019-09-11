package com.sap.cloud.lm.sl.cf.core.persistence.query.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.persistence.dto.HistoricOperationEventDto;
import com.sap.cloud.lm.sl.cf.core.persistence.dto.HistoricOperationEventDto.AttributeNames;
import com.sap.cloud.lm.sl.cf.core.persistence.query.HistoricOperationEventQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import com.sap.cloud.lm.sl.cf.core.persistence.query.criteria.QueryCriteria;
import com.sap.cloud.lm.sl.cf.core.persistence.service.HistoricOperationEventService.HistoricOperationEventMapper;

public class HistoricOperationEventQueryImpl extends AbstractQueryImpl<HistoricOperationEvent, HistoricOperationEventQuery>
    implements HistoricOperationEventQuery {

    private QueryCriteria queryCriteria = new QueryCriteria();
    private HistoricOperationEventMapper historicOperationEventFactory;

    public HistoricOperationEventQueryImpl(EntityManager entityManager, HistoricOperationEventMapper historicOperationEventFactory) {
        super(entityManager);
        this.historicOperationEventFactory = historicOperationEventFactory;
    }

    @Override
    public HistoricOperationEventQuery id(Long id) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(id)
                                                                       .build());
        return this;
    }

    @Override
    public HistoricOperationEventQuery processId(String processId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.PROCESS_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(processId)
                                                                       .build());
        return this;
    }

    @Override
    public HistoricOperationEventQuery type(EventType type) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.TYPE)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(type)
                                                                       .build());
        return this;
    }

    @Override
    public HistoricOperationEventQuery olderThan(Date time) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<Date> builder()
                                                                       .attribute(AttributeNames.TIMESTAMP)
                                                                       .condition(getCriteriaBuilder()::lessThan)
                                                                       .value(time)
                                                                       .build());
        return this;
    }

    @Override
    public HistoricOperationEvent singleResult() {
        HistoricOperationEventDto dto = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                    HistoricOperationEventDto.class).getSingleResult());
        return historicOperationEventFactory.fromDto(dto);
    }

    @Override
    public List<HistoricOperationEvent> list() {
        List<HistoricOperationEventDto> dtos = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                           HistoricOperationEventDto.class).getResultList());
        return dtos.stream()
                   .map(historicOperationEventFactory::fromDto)
                   .collect(Collectors.toList());
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria, HistoricOperationEventDto.class).executeUpdate());
    }

}