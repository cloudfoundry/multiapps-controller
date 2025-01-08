package org.cloudfoundry.multiapps.controller.persistence.services;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;

@Named
public class DatabaseMonitoringService {

    private static final String COUNT_ACQUIRED_LOCKS_BY_APPLICATION_INSTANCE_SQL_QUERY = "SELECT count(*) FROM pg_locks l JOIN pg_stat_activity a ON l.pid = a.pid WHERE NOT l.granted AND a.application_name = ?1";

    private final EntityManagerFactory entityManagerFactory;

    @Inject
    public DatabaseMonitoringService(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public long getProcessesWaitingForLocks(String applicationName) {
        EntityManager entityManager = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            Query query = entityManager.createNativeQuery(COUNT_ACQUIRED_LOCKS_BY_APPLICATION_INSTANCE_SQL_QUERY);
            query.setParameter(1, applicationName);
            Object result = query.getSingleResult();
            return ((Number) result).longValue();
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }
}
