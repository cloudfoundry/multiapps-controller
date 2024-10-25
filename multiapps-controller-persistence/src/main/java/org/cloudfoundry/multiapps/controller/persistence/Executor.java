package org.cloudfoundry.multiapps.controller.persistence;

import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

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
