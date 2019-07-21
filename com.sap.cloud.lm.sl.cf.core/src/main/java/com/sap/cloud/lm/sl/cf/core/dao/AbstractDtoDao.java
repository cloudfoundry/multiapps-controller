package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.List;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.DtoWithPrimaryKey;

public abstract class AbstractDtoDao<T extends DtoWithPrimaryKey<P>, P> {

    private EntityManagerFactory entityManagerFactory;

    public AbstractDtoDao(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public T findRequired(P primaryKey) {
        return execute(manager -> {
            T dto = findInternal(manager, primaryKey);
            if (dto == null) {
                onEntityNotFound(primaryKey);
            }
            return dto;
        });
    }

    public T find(P primaryKey) {
        return execute(manager -> findInternal(manager, primaryKey));
    }

    public void add(T dto) {
        try {
            executeInTransaction(manager -> {
                manager.persist(dto);
                return null;
            });
        } catch (RollbackException e) {
            onEntityConflict(dto, e);
        }
    }

    public void remove(P primaryKey) {
        executeInTransaction(manager -> {
            T dto = findInternal(manager, primaryKey);
            if (dto == null) {
                onEntityNotFound(primaryKey);
            }
            manager.remove(dto);
            return null;
        });
    }

    public void removeAll(List<T> dtos) {
        executeInTransaction(manager -> {
            for (T dto : dtos) {
                manager.remove(dto);
            }
            return null;
        });
    }

    public T update(P primaryKey, T newDto) {
        try {
            return executeInTransaction(manager -> {
                T existingDto = findInternal(manager, primaryKey);
                if (existingDto == null) {
                    onEntityNotFound(primaryKey);
                }
                T dto = merge(existingDto, newDto);
                manager.merge(dto);
                return dto;
            });
        } catch (RollbackException e) {
            onEntityConflict(newDto, e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<T> findAll() {
        return execute(manager -> manager.createNamedQuery(getFindAllNamedQuery())
            .getResultList());
    }

    protected T findInternal(EntityManager manager, P primaryKey) {
        return manager.find(getDtoClass(), primaryKey);
    }

    protected <R> R execute(Function<EntityManager, R> function) {
        return new Executor<R>(createEntityManager()).execute(function);
    }

    protected <R> R executeInTransaction(Function<EntityManager, R> function) {
        return new TransactionalExecutor<R>(createEntityManager()).execute(function);
    }

    protected EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    protected abstract T merge(T existingDto, T newDto);

    protected abstract void onEntityConflict(T dto, Throwable cause);

    protected abstract void onEntityNotFound(P primaryKey);

    protected abstract String getFindAllNamedQuery();

    protected abstract <C extends DtoWithPrimaryKey<P>> Class<C> getDtoClass();
}
