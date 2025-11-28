package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdownDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdownDto.AttributeNames;
import org.cloudfoundry.multiapps.controller.persistence.query.ApplicationShutdownQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;
import org.cloudfoundry.multiapps.controller.persistence.services.ApplicationShutdownService;

public class ApplicationShutdownQueryImpl extends AbstractQueryImpl<ApplicationShutdown, ApplicationShutdownQuery>
    implements ApplicationShutdownQuery {

    private final QueryCriteria queryCriteria = new QueryCriteria();
    private final ApplicationShutdownService.ApplicationShutdownMapper applicationShutdownMapper;

    public ApplicationShutdownQueryImpl(EntityManager entityManager,
                                        ApplicationShutdownService.ApplicationShutdownMapper applicationShutdownMapper) {
        super(entityManager);
        this.applicationShutdownMapper = applicationShutdownMapper;
    }

    @Override
    public ApplicationShutdownQuery applicationInstanceId(String applicationInstanceId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.APPLICATION_INSTANCE_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(applicationInstanceId)
                                                                       .build());
        return this;
    }

    @Override
    public ApplicationShutdownQuery applicationId(String applicationId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.APPLICATION_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(applicationId)
                                                                       .build());
        return this;
    }

    @Override
    public ApplicationShutdownQuery applicationInstanceIndex(int applicationInstanceIndex) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.APPLICATION_INSTANCE_INDEX)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(applicationInstanceIndex)
                                                                       .build());
        return this;
    }

    @Override
    public ApplicationShutdown singleResult() throws NoResultException, NonUniqueResultException {
        ApplicationShutdownDto dto = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                 ApplicationShutdownDto.class).getSingleResult());
        return applicationShutdownMapper.fromDto(dto);
    }

    @Override
    public List<ApplicationShutdown> list() {
        List<ApplicationShutdownDto> dtos = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                        ApplicationShutdownDto.class).getResultList());

        return dtos.stream()
                   .map(applicationShutdownMapper::fromDto)
                   .toList();
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria, ApplicationShutdown.class).executeUpdate());
    }
}
