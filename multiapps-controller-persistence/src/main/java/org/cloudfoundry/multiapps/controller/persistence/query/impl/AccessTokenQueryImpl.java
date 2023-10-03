package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.criteria.Expression;

import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.dto.AccessTokenDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.AccessTokenDto.AttributeNames;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.query.AccessTokenQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService.AccessTokenMapper;

public class AccessTokenQueryImpl extends AbstractQueryImpl<AccessToken, AccessTokenQuery> implements AccessTokenQuery {

    private final QueryCriteria queryCriteria = new QueryCriteria();
    private final AccessTokenMapper accessTokenMapper;

    public AccessTokenQueryImpl(EntityManager entityManager, AccessTokenMapper accessTokenMapper) {
        super(entityManager);
        this.accessTokenMapper = accessTokenMapper;
    }

    @Override
    public AccessTokenQuery id(Long id) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(id)
                                                                       .build());
        return this;
    }

    @Override
    public AccessTokenQuery withIdAnyOf(List<Long> ids) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<List<Long>> builder()
                                                                       .attribute(AttributeNames.ID)
                                                                       .condition(Expression::in)
                                                                       .value(ids)
                                                                       .build());
        return this;
    }

    @Override
    public AccessTokenQuery value(byte[] value) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.VALUE)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(value)
                                                                       .build());
        return this;
    }

    @Override
    public AccessTokenQuery username(String username) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.USERNAME)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(username)
                                                                       .build());
        return this;
    }

    @Override
    public AccessTokenQuery expiresBefore(LocalDateTime expiresAt) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<LocalDateTime> builder()
                                                                       .attribute(AttributeNames.EXPIRES_AT)
                                                                       .condition(getCriteriaBuilder()::lessThan)
                                                                       .value(expiresAt)
                                                                       .build());
        return this;
    }

    @Override
    public AccessTokenQuery orderByExpiresAt(OrderDirection orderDirection) {
        setOrder(AttributeNames.EXPIRES_AT, orderDirection);
        return this;
    }

    @Override
    public AccessToken singleResult() throws NoResultException, NonUniqueResultException {
        AccessTokenDto accessTokenDto = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                    AccessTokenDto.class).getSingleResult());
        return accessTokenMapper.fromDto(accessTokenDto);
    }

    @Override
    public List<AccessToken> list() {
        List<AccessTokenDto> accessTokenDtos = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                           AccessTokenDto.class).getResultList());
        return accessTokenDtos.stream()
                              .map(accessTokenMapper::fromDto)
                              .collect(Collectors.toList());
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria, AccessTokenDto.class).executeUpdate());
    }
}
