package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.criteria.Expression;

import org.cloudfoundry.multiapps.controller.persistence.dto.AsyncUploadJobDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.AsyncUploadJobDto.AttributeNames;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry.State;
import org.cloudfoundry.multiapps.controller.persistence.query.AsyncUploadJobsQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;
import org.cloudfoundry.multiapps.controller.persistence.services.AsyncUploadJobService.AsyncUploadJobMapper;

public class AsyncUploadJobsQueryImpl extends AbstractQueryImpl<AsyncUploadJobEntry, AsyncUploadJobsQuery> implements AsyncUploadJobsQuery {

    private final QueryCriteria queryCriteria = new QueryCriteria();
    private final AsyncUploadJobMapper mapper = new AsyncUploadJobMapper();

    public AsyncUploadJobsQueryImpl(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public AsyncUploadJobsQuery id(String id) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(id)
                                                                       .build());
        return this;
    }

    @Override
    public AsyncUploadJobsQuery spaceGuid(String spaceGuid) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.SPACE_GUID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(spaceGuid)
                                                                       .build());
        return this;
    }

    @Override
    public AsyncUploadJobsQuery state(AsyncUploadJobEntry.State state) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.STATE)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(state)
                                                                       .build());
        return this;
    }

    @Override
    public AsyncUploadJobsQuery namespace(String namespace) {
        if (namespace == null) {
            return this;
        }
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.NAMESPACE)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(namespace)
                                                                       .build());
        return this;
    }

    @Override
    public AsyncUploadJobsQuery user(String user) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.USER)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(user)
                                                                       .build());
        return this;
    }

    @Override
    public AsyncUploadJobsQuery url(String url) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.URL)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(url)
                                                                       .build());
        return this;
    }

    @Override
    public AsyncUploadJobsQuery withoutFinishedAt() {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.FINISHED_AT)
                                                                       .condition((attribute, value) -> attribute.isNull())
                                                                       .build());
        return this;
    }

    @Override
    public AsyncUploadJobsQuery withStateAnyOf(State... states) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<List<State>> builder()
                                                                       .attribute(AttributeNames.STATE)
                                                                       .condition(Expression::in)
                                                                       .value(Arrays.asList(states))
                                                                       .build());
        return this;
    }

    @Override
    public AsyncUploadJobsQuery addedBefore(LocalDateTime addedBefore) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<LocalDateTime> builder()
                                                                       .attribute(AttributeNames.ADDED_AT)
                                                                       .condition(getCriteriaBuilder()::lessThan)
                                                                       .value(addedBefore)
                                                                       .build());
        return this;
    }

    @Override
    public AsyncUploadJobsQuery startedBefore(LocalDateTime startedBefore) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<LocalDateTime> builder()
                                                                       .attribute(AttributeNames.STARTED_AT)
                                                                       .condition(getCriteriaBuilder()::lessThan)
                                                                       .value(startedBefore)
                                                                       .build());
        return this;
    }

    @Override
    public AsyncUploadJobsQuery withoutStartedAt() {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.STARTED_AT)
                                                                       .condition((attribute, value) -> attribute.isNull())
                                                                       .build());
        return this;
    }

    @Override
    public AsyncUploadJobsQuery withoutAddedAt() {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ADDED_AT)
                                                                       .condition((attribute, value) -> attribute.isNull())
                                                                       .build());
        return this;
    }

    @Override
    public AsyncUploadJobsQuery instanceIndex(int instanceIndex) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.INSTANCE_INDEX)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(instanceIndex)
                                                                       .build());
        return this;

    }

    @Override
    public AsyncUploadJobEntry singleResult() throws NoResultException, NonUniqueResultException {
        AsyncUploadJobDto dto = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                            AsyncUploadJobDto.class).getSingleResult());
        return mapper.fromDto(dto);
    }

    @Override
    public List<AsyncUploadJobEntry> list() {
        List<AsyncUploadJobDto> dtos = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                   AsyncUploadJobDto.class).getResultList());
        return dtos.stream()
                   .map(mapper::fromDto)
                   .collect(Collectors.toList());
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria, AsyncUploadJobDto.class).executeUpdate());
    }
}
