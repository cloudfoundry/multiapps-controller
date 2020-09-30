package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.dto.ProgressMessageDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.ProgressMessageDto.AttributeNames;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage.ProgressMessageType;
import org.cloudfoundry.multiapps.controller.persistence.query.ProgressMessageQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService.ProgressMessageMapper;

public class ProgressMessageQueryImpl extends AbstractQueryImpl<ProgressMessage, ProgressMessageQuery> implements ProgressMessageQuery {

    private final QueryCriteria queryCriteria = new QueryCriteria();
    private final ProgressMessageMapper progressMessageFactory;

    public ProgressMessageQueryImpl(EntityManager entityManager, ProgressMessageMapper progressMessageFactory) {
        super(entityManager);
        this.progressMessageFactory = progressMessageFactory;
    }

    @Override
    public ProgressMessageQuery id(Long id) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(id)
                                                                       .build());
        return this;
    }

    @Override
    public ProgressMessageQuery processId(String processId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.PROCESS_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(processId)
                                                                       .build());
        return this;
    }

    @Override
    public ProgressMessageQuery taskId(String taskId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.TASK_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(taskId)
                                                                       .build());
        return this;
    }

    @Override
    public ProgressMessageQuery type(ProgressMessageType type) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.TYPE)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(type)
                                                                       .build());
        return this;
    }

    @Override
    public ProgressMessageQuery typeNot(ProgressMessageType type) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.TYPE)
                                                                       .condition(getCriteriaBuilder()::notEqual)
                                                                       .value(type)
                                                                       .build());
        return this;
    }

    @Override
    public ProgressMessageQuery text(String text) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.TEXT)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(text)
                                                                       .build());
        return this;
    }

    @Override
    public ProgressMessageQuery olderThan(Date time) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<Date> builder()
                                                                       .attribute(AttributeNames.TIMESTAMP)
                                                                       .condition(getCriteriaBuilder()::lessThan)
                                                                       .value(time)
                                                                       .build());
        return this;
    }

    @Override
    public ProgressMessageQuery orderById(OrderDirection orderDirection) {
        setOrder(AttributeNames.ID, orderDirection);
        return this;
    }

    @Override
    public ProgressMessage singleResult() {
        ProgressMessageDto dto = executeInTransaction(manager -> createQuery(manager, queryCriteria, getDtoClass()).getSingleResult());
        return progressMessageFactory.fromDto(dto);
    }

    @Override
    public List<ProgressMessage> list() {
        return executeInTransaction(manager -> createQuery(manager, queryCriteria, getDtoClass()).getResultList()).stream()
                                                                                                                  .map(progressMessageFactory::fromDto)
                                                                                                                  .collect(Collectors.toList());
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria, getDtoClass()).executeUpdate());
    }

    protected Class<? extends ProgressMessageDto> getDtoClass() {
        return ProgressMessageDto.class;
    }

}