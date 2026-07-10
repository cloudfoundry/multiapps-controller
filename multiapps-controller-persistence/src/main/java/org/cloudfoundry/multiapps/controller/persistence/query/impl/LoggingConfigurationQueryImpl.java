package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import org.cloudfoundry.multiapps.controller.persistence.dto.LoggingConfigurationDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.LoggingConfigurationDto.AttributeNames;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.query.LoggingConfigurationQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;
import org.cloudfoundry.multiapps.controller.persistence.services.cloudlogging.CloudLoggingServiceConfigurationService.LoggingConfigurationMapper;

public class LoggingConfigurationQueryImpl extends AbstractQueryImpl<LoggingConfiguration, LoggingConfigurationQuery>
    implements LoggingConfigurationQuery {

    private final QueryCriteria queryCriteria = new QueryCriteria();
    private final LoggingConfigurationMapper mapper;

    public LoggingConfigurationQueryImpl(EntityManager entityManager, LoggingConfigurationMapper mapper) {
        super(entityManager);
        this.mapper = mapper;
    }

    @Override
    public LoggingConfigurationQuery id(String id) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(id)
                                                                       .build());
        return this;
    }

    @Override
    public LoggingConfigurationQuery mtaSpace(String mtaSpace) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.MTA_SPACE)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(mtaSpace)
                                                                       .build());
        return this;
    }

    @Override
    public LoggingConfigurationQuery mtaId(String mtaId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.MTA_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(mtaId)
                                                                       .build());
        return this;
    }

    @Override
    public LoggingConfigurationQuery mtaSpaceId(String mtaSpaceId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.MTA_SPACE_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(mtaSpaceId)
                                                                       .build());
        return this;
    }

    @Override
    public LoggingConfigurationQuery namespace(String namespace) {
        if (namespace == null) {
            queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                           .attribute(AttributeNames.NAMESPACE)
                                                                           .condition((attribute, value) -> attribute.isNull())
                                                                           .build());
        } else {
            queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                           .attribute(AttributeNames.NAMESPACE)
                                                                           .condition(getCriteriaBuilder()::equal)
                                                                           .value(namespace)
                                                                           .build());
        }
        return this;
    }

    @Override
    public LoggingConfiguration singleResult() throws NoResultException, NonUniqueResultException {
        LoggingConfigurationDto dto = executeInTransaction(
            manager -> createQuery(manager, queryCriteria, LoggingConfigurationDto.class).getSingleResult());
        return mapper.fromDto(dto);
    }

    @Override
    public List<LoggingConfiguration> list() {
        List<LoggingConfigurationDto> dtos = executeInTransaction(
            manager -> createQuery(manager, queryCriteria, LoggingConfigurationDto.class).getResultList());
        return dtos.stream()
                   .map(mapper::fromDto)
                   .collect(Collectors.toList());
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria, LoggingConfigurationDto.class).executeUpdate());
    }
}
