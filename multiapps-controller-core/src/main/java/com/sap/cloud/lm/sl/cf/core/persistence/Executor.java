package com.sap.cloud.lm.sl.cf.core.persistence;

import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

public class Executor<T> {

    protected final EntityManager manager;

    public Executor(EntityManager manager) {
        this.manager = manager;
    }

    public T execute(Function<EntityManager, T> function) {
        try {
            return function.apply(manager);
        } finally {
            cleanUp();
        }
    }

    protected void cleanUp() {
        if (manager != null) {
            try {
                rollbackTransactionIfActive();
            } finally {
                manager.close();
            }
        }
    }

    protected void rollbackTransactionIfActive() {
        EntityTransaction transaction = manager.getTransaction();

        if (transaction != null && transaction.isActive()) {
            transaction.rollback();
        }
    }

}
