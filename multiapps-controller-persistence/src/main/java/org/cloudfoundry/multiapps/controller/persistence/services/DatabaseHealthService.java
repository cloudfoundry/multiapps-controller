package org.cloudfoundry.multiapps.controller.persistence.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;

@Named
public class DatabaseHealthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHealthService.class);

    private final EntityManagerFactory entityManagerFactory;

    @Inject
    public DatabaseHealthService(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public Boolean isDatabaseHealthy() {
        EntityManager entityManager = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            Query query = entityManager.createNativeQuery("SELECT 1");
            query.getSingleResult();
            return true;
        } catch (Exception e) {
            LOGGER.error("Database health check failed", e);
            return false;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

}
