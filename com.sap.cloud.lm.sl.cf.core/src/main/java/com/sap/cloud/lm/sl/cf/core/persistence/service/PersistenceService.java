package com.sap.cloud.lm.sl.cf.core.persistence.service;

import java.text.MessageFormat;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.persistence.TransactionalExecutor;
import com.sap.cloud.lm.sl.cf.core.persistence.dto.DtoWithPrimaryKey;

public abstract class PersistenceService<T, D extends DtoWithPrimaryKey<P>, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceService.class);

    private final EntityManagerFactory entityManagerFactory;

    public PersistenceService(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public T add(T object) {
        D dto = getPersistenceObjectMapper().toDto(object);
        try {
            D newDto = executeInTransaction(manager -> {
                manager.persist(dto);
                return dto;
            });
            return getPersistenceObjectMapper().fromDto(newDto);
        } catch (RollbackException e) {
            LOGGER.error(MessageFormat.format(Messages.ERROR_WHILE_EXECUTING_TRANSACTION, e.getMessage()));
            onEntityConflict(dto, e);
        }
        return null;
    }

    public T update(P primaryKey, T newObject) {
        D newDto = getPersistenceObjectMapper().toDto(newObject);
        try {
            return executeInTransaction(manager -> update(primaryKey, newDto, manager));
        } catch (RollbackException e) {
            LOGGER.error(MessageFormat.format(Messages.ERROR_WHILE_EXECUTING_TRANSACTION, e.getMessage()));
            onEntityConflict(newDto, e);
        }
        return null;
    }

    private <R> R executeInTransaction(Function<EntityManager, R> function) {
        return new TransactionalExecutor<R>(createEntityManager()).execute(function);
    }

    protected EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    private T update(P primaryKey, D newDto, EntityManager manager) {
        @SuppressWarnings("unchecked")
        D existingDto = manager.find((Class<D>) newDto.getClass(), primaryKey);
        if (existingDto == null) {
            onEntityNotFound(primaryKey);
        }
        D dto = merge(existingDto, newDto);
        manager.merge(dto);
        return getPersistenceObjectMapper().fromDto(dto);
    }

    protected D merge(D existingPersistenceObject, D newPersistenceObject) {
        newPersistenceObject.setPrimaryKey(existingPersistenceObject.getPrimaryKey());
        return newPersistenceObject;
    }

    protected abstract PersistenceObjectMapper<T, D> getPersistenceObjectMapper();

    protected abstract void onEntityConflict(D dto, Throwable t);

    protected abstract void onEntityNotFound(P primaryKey);

}