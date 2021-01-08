package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.dto.OperationDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.OperationDto.AttributeNames;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService.OperationMapper;

public class OperationQueryImpl extends AbstractQueryImpl<Operation, OperationQuery> implements OperationQuery {

    private final QueryCriteria queryCriteria = new QueryCriteria();
    private final OperationMapper operationFactory;

    public OperationQueryImpl(EntityManager entityManager, OperationMapper operationFactory) {
        super(entityManager);
        this.operationFactory = operationFactory;
    }

    @Override
    public OperationQuery processId(String processId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.PROCESS_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(processId)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery processType(ProcessType processType) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.PROCESS_TYPE)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(processType)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery user(String user) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.USER)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(user)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery spaceId(String spaceId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.SPACE_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(spaceId)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery mtaId(String mtaId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.MTA_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(mtaId)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery namespace(String namespace) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.NAMESPACE)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(namespace)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery acquiredLock(Boolean acquiredLock) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ACQUIRED_LOCK)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(acquiredLock)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery state(Operation.State finalState) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.FINAL_STATE)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(finalState)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery cachedState(Operation.State currentState) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.CURRENT_STATE)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(currentState)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery startedBefore(Date startedBefore) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<Date> builder()
                                                                       .attribute(AttributeNames.STARTED_AT)
                                                                       .condition(getCriteriaBuilder()::lessThan)
                                                                       .value(startedBefore)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery endedAfter(Date endedAfter) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<Date> builder()
                                                                       .attribute(AttributeNames.ENDED_AT)
                                                                       .condition(getCriteriaBuilder()::greaterThan)
                                                                       .value(endedAfter)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery inNonFinalState() {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.FINAL_STATE)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(null)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery inFinalState() {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.FINAL_STATE)
                                                                       .condition(getCriteriaBuilder()::notEqual)
                                                                       .value(null)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery withStateAnyOf(List<Operation.State> states) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<List<Operation.State>> builder()
                                                                       .attribute(AttributeNames.FINAL_STATE)
                                                                       .condition(Expression::in)
                                                                       .value(states)
                                                                       .build());
        return this;
    }

    @Override
    public OperationQuery orderByProcessId(OrderDirection orderDirection) {
        setOrder(OperationDto.AttributeNames.PROCESS_ID, orderDirection);
        return this;
    }

    @Override
    public OperationQuery orderByEndTime(OrderDirection orderDirection) {
        setOrder(OperationDto.AttributeNames.ENDED_AT, orderDirection);
        return this;
    }

    @Override
    public OperationQuery orderByStartTime(OrderDirection orderDirection) {
        setOrder(OperationDto.AttributeNames.STARTED_AT, orderDirection);
        return this;
    }

    @Override
    public Operation singleResult() {
        OperationDto dto = executeInTransaction(manager -> createQuery(manager, queryCriteria, OperationDto.class).getSingleResult());
        return operationFactory.fromDto(dto);
    }

    @Override
    public List<Operation> list() {
        List<OperationDto> dtos = executeInTransaction(manager -> createQuery(manager, queryCriteria, OperationDto.class).getResultList());
        return dtos.stream()
                   .map(operationFactory::fromDto)
                   .collect(Collectors.toList());
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria, OperationDto.class).executeUpdate());
    }

}