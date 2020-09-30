package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.cloudfoundry.multiapps.controller.persistence.dto.HistoricOperationEventDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.HistoricOperationEventDto.AttributeNames;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.query.HistoricOperationEventQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService.HistoricOperationEventMapper;

public class HistoricOperationEventQueryImpl extends AbstractQueryImpl<HistoricOperationEvent, HistoricOperationEventQuery>
    implements HistoricOperationEventQuery {

    private final QueryCriteria queryCriteria = new QueryCriteria();
    private final HistoricOperationEventMapper historicOperationEventFactory;

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
    public HistoricOperationEventQuery type(HistoricOperationEvent.EventType type) {
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