package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import org.cloudfoundry.multiapps.controller.persistence.dto.SecretTokenDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.SecretTokenDto.AttributeNames;
import org.cloudfoundry.multiapps.controller.persistence.model.SecretToken;
import org.cloudfoundry.multiapps.controller.persistence.query.SecretTokenQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;
import org.cloudfoundry.multiapps.controller.persistence.services.SecretTokenService.SecretTokenMapper;

public class SecretTokenQueryImpl extends AbstractQueryImpl<SecretToken, SecretTokenQuery> implements SecretTokenQuery {

    private final QueryCriteria queryCriteria = new QueryCriteria();
    private final SecretTokenMapper secretTokenMapper;

    public SecretTokenQueryImpl(EntityManager entityManager, SecretTokenMapper secretTokenMapper) {
        super(entityManager);
        this.secretTokenMapper = secretTokenMapper;
    }

    @Override
    public SecretTokenQuery id(Long id) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(id)
                                                                       .build());
        return this;
    }

    @Override
    public SecretTokenQuery processInstanceId(String processInstanceId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.PROCESS_INSTANCE_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(processInstanceId)
                                                                       .build());
        return this;
    }

    @Override
    public SecretTokenQuery variableName(String variableName) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.VARIABLE_NAME)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(variableName)
                                                                       .build());
        return this;
    }

    @Override
    public SecretTokenQuery olderThan(LocalDateTime time) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<LocalDateTime> builder()
                                                                       .attribute(AttributeNames.TIMESTAMP)
                                                                       .condition(getCriteriaBuilder()::lessThan)
                                                                       .value(time)
                                                                       .build());
        return this;
    }

    @Override
    public SecretTokenQuery keyId(String keyId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.KEY_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(keyId)
                                                                       .build());
        return this;
    }

    @Override
    public SecretToken singleResult() throws NoResultException, NonUniqueResultException {
        SecretTokenDto secretTokenDto = executeInTransaction(
            entityManager -> createQuery(entityManager, queryCriteria, SecretTokenDto.class).getSingleResult());
        return secretTokenMapper.fromDto(secretTokenDto);
    }

    @Override
    public List<SecretToken> list() {
        List<SecretTokenDto> secretTokenDtos = executeInTransaction(
            entityManager -> createQuery(entityManager, queryCriteria, SecretTokenDto.class).getResultList());

        return secretTokenDtos.stream()
                              .map(secretTokenMapper::fromDto)
                              .collect(Collectors.toList());
    }

    @Override
    public int delete() {
        return executeInTransaction(entityManager -> createDeleteQuery(entityManager, queryCriteria, SecretTokenDto.class).executeUpdate());
    }

}
