package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;

import org.cloudfoundry.multiapps.controller.persistence.dto.AsyncUploadJobDto.AttributeNames;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry.State;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryAttributeRestriction;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AsyncUploadJobsQueryImplTest {

    private AsyncUploadJobsQueryImpl query;

    @BeforeEach
    void setUp() {
        EntityManager entityManager = Mockito.mock(EntityManager.class);
        CriteriaBuilder criteriaBuilder = Mockito.mock(CriteriaBuilder.class);
        Mockito.when(entityManager.getCriteriaBuilder())
               .thenReturn(criteriaBuilder);

        query = new AsyncUploadJobsQueryImpl(entityManager);
    }

    @Test
    void testIdAddsRestrictionAndReturnsThis() {
        Assertions.assertSame(query, query.id("job-1"));

        assertSingleRestriction(AttributeNames.ID, "job-1");
    }

    @Test
    void testSpaceGuidAddsRestriction() {
        query.spaceGuid("space-guid");

        assertSingleRestriction(AttributeNames.SPACE_GUID, "space-guid");
    }

    @Test
    void testStateAddsRestriction() {
        query.state(State.RUNNING);

        assertSingleRestriction(AttributeNames.STATE, State.RUNNING);
    }

    @Test
    void testNamespaceAddsRestrictionWhenNonNull() {
        query.namespace("ns");

        assertSingleRestriction(AttributeNames.NAMESPACE, "ns");
    }

    @Test
    void testNamespaceIsNoopForNull() {
        Assertions.assertSame(query, query.namespace(null));

        Assertions.assertEquals(0, getRestrictions().size());
    }

    @Test
    void testUserAddsRestriction() {
        query.user("alice");

        assertSingleRestriction(AttributeNames.USER, "alice");
    }

    @Test
    void testUrlAddsRestriction() {
        query.url("https://example.com/app.mtar");

        assertSingleRestriction(AttributeNames.URL, "https://example.com/app.mtar");
    }

    @Test
    void testWithoutFinishedAtAddsNullRestrictionForFinishedAt() {
        query.withoutFinishedAt();

        assertSingleRestrictionAttribute(AttributeNames.FINISHED_AT);
    }

    @Test
    void testWithStateAnyOfAddsRestrictionWithListOfStates() {
        query.withStateAnyOf(State.RUNNING, State.FINISHED);

        assertSingleRestriction(AttributeNames.STATE, List.of(State.RUNNING, State.FINISHED));
    }

    @Test
    void testAddedBeforeAddsRestriction() {
        LocalDateTime before = LocalDateTime.parse("2026-05-01T10:00:00");

        query.addedBefore(before);

        assertSingleRestriction(AttributeNames.ADDED_AT, before);
    }

    @Test
    void testStartedBeforeAddsRestriction() {
        LocalDateTime before = LocalDateTime.parse("2026-05-01T10:00:00");

        query.startedBefore(before);

        assertSingleRestriction(AttributeNames.STARTED_AT, before);
    }

    @Test
    void testWithoutStartedAtAddsRestriction() {
        query.withoutStartedAt();

        assertSingleRestrictionAttribute(AttributeNames.STARTED_AT);
    }

    @Test
    void testWithoutAddedAtAddsRestriction() {
        query.withoutAddedAt();

        assertSingleRestrictionAttribute(AttributeNames.ADDED_AT);
    }

    @Test
    void testInstanceIndexAddsRestriction() {
        query.instanceIndex(3);

        assertSingleRestriction(AttributeNames.INSTANCE_INDEX, 3);
    }

    @Test
    void testWithFileIdsAddsRestriction() {
        List<String> fileIds = List.of("f1", "f2");

        query.withFileIds(fileIds);

        assertSingleRestriction(AttributeNames.FILE_ID, fileIds);
    }

    @Test
    void testFluentBuilderCombinesMultipleRestrictions() {
        query.id("job-1")
             .spaceGuid("space-guid")
             .user("alice");

        Assertions.assertEquals(3, getRestrictions().size());
    }

    private void assertSingleRestriction(String expectedAttribute, Object expectedValue) {
        Set<QueryAttributeRestriction<?>> restrictions = getRestrictions();
        Assertions.assertEquals(1, restrictions.size());
        QueryAttributeRestriction<?> restriction = restrictions.iterator()
                                                               .next();
        Assertions.assertEquals(expectedAttribute, restriction.getAttribute());
        Assertions.assertEquals(expectedValue, restriction.getValue());
    }

    private void assertSingleRestrictionAttribute(String expectedAttribute) {
        Set<QueryAttributeRestriction<?>> restrictions = getRestrictions();
        Assertions.assertEquals(1, restrictions.size());
        QueryAttributeRestriction<?> restriction = restrictions.iterator()
                                                               .next();
        Assertions.assertEquals(expectedAttribute, restriction.getAttribute());
    }

    @SuppressWarnings("unchecked")
    private Set<QueryAttributeRestriction<?>> getRestrictions() {
        try {
            Field criteriaField = AsyncUploadJobsQueryImpl.class.getDeclaredField("queryCriteria");
            criteriaField.setAccessible(true);
            QueryCriteria criteria = (QueryCriteria) criteriaField.get(query);

            Field restrictionsField = QueryCriteria.class.getDeclaredField("attributeRestrictions");
            restrictionsField.setAccessible(true);
            return (Set<QueryAttributeRestriction<?>>) restrictionsField.get(criteria);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
