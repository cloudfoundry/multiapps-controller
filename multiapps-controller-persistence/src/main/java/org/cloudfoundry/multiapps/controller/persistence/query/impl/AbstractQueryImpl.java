package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.TransactionalExecutor;
import org.cloudfoundry.multiapps.controller.persistence.query.Query;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;

public abstract class AbstractQueryImpl<R, T extends Query<R, T>> implements Query<R, T> {

    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;
    private Integer limit;
    private Integer offset;
    private OrderDirection orderDirection;
    private String orderAttribute;

    protected AbstractQueryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.criteriaBuilder = entityManager.getCriteriaBuilder();
    }

    @Override
    public T limitOnSelect(int limit) {
        this.limit = limit;
        return getSelf();
    }

    @Override
    public T offsetOnSelect(int offset) {
        this.offset = offset;
        return getSelf();
    }

    protected T setOrder(String orderAttribute, OrderDirection orderDirection) {
        this.orderAttribute = orderAttribute;
        this.orderDirection = orderDirection;
        return getSelf();
    }

    private <E> void applyLimitAndOffset(TypedQuery<E> typedQuery) {
        if (limit != null) {
            typedQuery.setMaxResults(limit);
        }
        if (offset != null) {
            typedQuery.setFirstResult(offset);
        }
    }

    private <E> CriteriaQuery<E> applyOrder(CriteriaQuery<E> criteriaQuery, Root<E> root) {
        if (orderAttribute == null) {
            return criteriaQuery;
        }
        if (orderDirection == OrderDirection.ASCENDING) {
            return criteriaQuery.orderBy(criteriaBuilder.asc(root.get(orderAttribute)));
        }
        return criteriaQuery.orderBy(criteriaBuilder.desc(root.get(orderAttribute)));
    }

    protected <E> TypedQuery<E> createQuery(EntityManager entityManager, QueryCriteria criteria, Class<E> dtoClass) {
        CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(dtoClass);
        Root<E> root = criteriaQuery.from(dtoClass);
        criteriaQuery.where(criteria.toQueryPredicates(root)
                                    .toArray(new Predicate[0]));
        criteriaQuery = applyOrder(criteriaQuery, root);
        TypedQuery<E> typedQuery = entityManager.createQuery(criteriaQuery);
        applyLimitAndOffset(typedQuery);
        return typedQuery;
    }

    protected <E> jakarta.persistence.Query createDeleteQuery(EntityManager entityManager, QueryCriteria criteria, Class<E> dtoClass) {
        CriteriaDelete<E> deleteQuery = criteriaBuilder.createCriteriaDelete(dtoClass);
        Root<E> root = deleteQuery.from(dtoClass);
        deleteQuery.where(criteria.toQueryPredicates(root)
                                  .toArray(new Predicate[0]));
        return entityManager.createQuery(deleteQuery);
    }

    protected <E> E executeInTransaction(Function<EntityManager, E> function) {
        return new TransactionalExecutor<E>(entityManager).execute(function);
    }

    protected CriteriaBuilder getCriteriaBuilder() {
        return criteriaBuilder;
    }

    @SuppressWarnings("unchecked")
    public T getSelf() {
        return (T) this;
    }
}