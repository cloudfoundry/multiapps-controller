package org.cloudfoundry.multiapps.controller.persistence.services;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.Messages;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;

@Named
public class DatabaseHealthService {

    private static final String SELECT_1_QUERY = "SELECT 1";

    private final EntityManagerFactory entityManagerFactory;

    @Inject
    public DatabaseHealthService(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public void testDatabaseConnection() {
        EntityManager entityManager = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            Query query = entityManager.createNativeQuery(SELECT_1_QUERY);
            query.getSingleResult();
        } catch (Exception e) {
            throw new SLException(e, Messages.DATABASE_HEALTH_CHECK_FAILED);
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

}
