package org.cloudfoundry.multiapps.controller.persistence.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;

class DatabaseMonitoringServiceTest {

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private Query query;
    private DatabaseMonitoringService databaseMonitoringService;

    @BeforeEach
    void setUp() {
        entityManagerFactory = mock(EntityManagerFactory.class);
        entityManager = mock(EntityManager.class);
        query = mock(Query.class);
        databaseMonitoringService = new DatabaseMonitoringService(entityManagerFactory);

        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(10L);
    }

    @Test
    void testGetProcessesWaitingForLocks() {
        long result = databaseMonitoringService.getProcessesWaitingForLocks("testApp");

        assertEquals(10L, result);
        verify(entityManager).close();
    }

}
