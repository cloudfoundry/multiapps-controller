package com.sap.cloud.lm.sl.cf.core.persistence;

import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

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
