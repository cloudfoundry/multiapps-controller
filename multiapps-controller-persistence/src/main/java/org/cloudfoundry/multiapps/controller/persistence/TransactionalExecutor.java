package org.cloudfoundry.multiapps.controller.persistence;

import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class TransactionalExecutor<T> extends Executor<T> {

    public TransactionalExecutor(EntityManager manager) {
        super(manager);
    }

    @Override
    public T execute(Function<EntityManager, T> function) {
        T result = null;
        try {
            EntityTransaction transaction = manager.getTransaction();
            transaction.begin();

            result = function.apply(manager);

            transaction.commit();
        } finally {
            cleanUp();
        }
        return result;
    }

}
