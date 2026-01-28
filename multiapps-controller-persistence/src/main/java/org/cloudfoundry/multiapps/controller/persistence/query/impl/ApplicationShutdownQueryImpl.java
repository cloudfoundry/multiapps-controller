package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.util.Date;
import java.util.List;

import jakarta.persistence.EntityManager;
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
    public ApplicationShutdownQuery id(String instanceId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(instanceId)
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
    public ApplicationShutdownQuery shutdownStatus(ApplicationShutdown.Status shutdownStatus) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.SHUTDOWN_STATUS)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(shutdownStatus)
                                                                       .build());
        return this;
    }

    @Override
    public ApplicationShutdownQuery startedAt(Date startedAt) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.STARTED_AT)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(startedAt)
                                                                       .build());
        return this;
    }

    @Override
    public ApplicationShutdown singleResult() {
        ApplicationShutdownDto dto = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                 ApplicationShutdownDto.class).getResultList()
                                                                                                              .stream()
                                                                                                              .findFirst()
                                                                                                              .orElse(null));
        return dto != null ? applicationShutdownMapper.fromDto(dto) : null;
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
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria, ApplicationShutdownDto.class).executeUpdate());
    }
}
