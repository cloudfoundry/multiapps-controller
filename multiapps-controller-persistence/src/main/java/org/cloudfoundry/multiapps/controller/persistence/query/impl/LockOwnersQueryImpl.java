package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.dto.LockOwnerDto;
import org.cloudfoundry.multiapps.controller.persistence.model.LockOwnerEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.LockOwnersQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;
import org.cloudfoundry.multiapps.controller.persistence.services.LockOwnerService.LockOwnersMapper;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.criteria.Expression;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class LockOwnersQueryImpl extends AbstractQueryImpl<LockOwnerEntry, LockOwnersQuery> implements LockOwnersQuery {

    private final QueryCriteria queryCriteria = new QueryCriteria();
    private final LockOwnersMapper lockOwnersMapper = new LockOwnersMapper();

    public LockOwnersQueryImpl(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public LockOwnersQuery id(Long id) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(LockOwnerDto.AttributeNames.ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(id)
                                                                       .build());
        return this;
    }

    @Override
    public LockOwnersQuery lockOwner(String lockOwner) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(LockOwnerDto.AttributeNames.LOCK_OWNER)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(lockOwner)
                                                                       .build());
        return this;
    }

    @Override
    public LockOwnersQuery withLockOwnerAnyOf(List<String> lockOwners) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<List<String>> builder()
                                                                       .attribute(LockOwnerDto.AttributeNames.LOCK_OWNER)
                                                                       .condition(Expression::in)
                                                                       .value(lockOwners)
                                                                       .build());
        return this;
    }

    @Override
    public LockOwnersQuery olderThan(LocalDateTime timestamp) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<LocalDateTime> builder()
                                                                       .attribute(LockOwnerDto.AttributeNames.TIMESTAMP)
                                                                       .condition(getCriteriaBuilder()::lessThan)
                                                                       .value(timestamp)
                                                                       .build());
        return this;
    }

    @Override
    public LockOwnersQuery orderByTimestamp(OrderDirection orderDirection) {
        setOrder(LockOwnerDto.AttributeNames.TIMESTAMP, orderDirection);
        return this;
    }

    @Override
    public LockOwnerEntry singleResult() throws NoResultException, NonUniqueResultException {
        LockOwnerDto dto = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                       LockOwnerDto.class).getSingleResult());
        return lockOwnersMapper.fromDto(dto);
    }

    @Override
    public List<LockOwnerEntry> list() {
        List<LockOwnerDto> dtos = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                              LockOwnerDto.class).getResultList());
        return dtos.stream()
                   .map(lockOwnersMapper::fromDto)
                   .collect(Collectors.toList());
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria, LockOwnerDto.class).executeUpdate());
    }
}
