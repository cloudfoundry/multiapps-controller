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

class DatabaseHealthServiceTest {

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private Query query;
    private DatabaseHealthService databaseHealthService;

    @BeforeEach
    void setUp() {
        entityManagerFactory = mock(EntityManagerFactory.class);
        entityManager = mock(EntityManager.class);
        query = mock(Query.class);
        databaseHealthService = new DatabaseHealthService(entityManagerFactory);

        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(10);
    }

    @Test
    void testDatabaseConnection() {
        databaseHealthService.testDatabaseConnection();

        verify(entityManager).close();
        assertEquals(10, query.getSingleResult());
    }
}
