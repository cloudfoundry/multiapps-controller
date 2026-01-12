package org.cloudfoundry.multiapps.controller.persistence.services;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.query.ApplicationShutdownQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationShutdownServiceTest {

    private final String APPLICATION_ID = UUID.randomUUID()
                                              .toString();
    private final String INSTANCE_ID = UUID.randomUUID()
                                           .toString();
    private final String INSTANCE_ID_2 = UUID.randomUUID()
                                             .toString();
    private final ApplicationShutdown APPLICATION_SHUTDOWN = createApplicationShutdownInstance(INSTANCE_ID, 0);
    private final ApplicationShutdown APPLICATION_SHUTDOWN_2 = createApplicationShutdownInstance(INSTANCE_ID_2, 1);

    private final ApplicationShutdownService applicationShutdownService = createApplicationShutdownService();

    @AfterEach
    void cleanUp() {
        applicationShutdownService.createQuery()
                                  .delete();
    }

    @Test
    void testAdd() {
        applicationShutdownService.add(APPLICATION_SHUTDOWN);
        assertEquals(1, applicationShutdownService.createQuery()
                                                  .list()
                                                  .size());
        
        assertEquals(APPLICATION_SHUTDOWN.getId(), applicationShutdownService.createQuery()
                                                                             .id(APPLICATION_SHUTDOWN.getId())
                                                                             .singleResult()
                                                                             .getId());

    }

    @Test
    void testAddWithAlreadyExistingApplicationShutdown() {
        applicationShutdownService.add(APPLICATION_SHUTDOWN);
        assertApplicationShutdownExists(APPLICATION_SHUTDOWN.getId());

        assertThrows(ConflictException.class, () -> applicationShutdownService.add(APPLICATION_SHUTDOWN));
    }

    @Test
    void testAddWithMoreThanOneApplicationShutdown() {
        addApplicationShutdown(List.of(APPLICATION_SHUTDOWN, APPLICATION_SHUTDOWN_2));
        assertApplicationShutdownExists(APPLICATION_SHUTDOWN.getId());
        assertApplicationShutdownExists(APPLICATION_SHUTDOWN_2.getId());

        assertEquals(2, applicationShutdownService.createQuery()
                                                  .list()
                                                  .size());
    }

    @Test
    void testQueryById() {
        testQueryByCriteria((query, applicationShutdown) -> query.id(applicationShutdown.getId()), 1);
    }

    @Test
    void testQueryByApplicationId() {
        testQueryByCriteria((query, applicationShutdown) -> query.applicationId(applicationShutdown.getApplicationId()), 2);
    }

    @Test
    void testQueryByShutdownStatus() {
        testQueryByCriteria((query, applicationShutdown) -> query.shutdownStatus(applicationShutdown.getStatus()), 2);
    }

    @Test
    void testQueryByApplicationInstanceIndex() {
        testQueryByCriteria(
            (query, applicationShutdown) -> query.applicationInstanceIndex(applicationShutdown.getApplicationInstanceIndex()), 1);
    }

    @Test
    void testQueryByStartedAt() {
        testQueryByCriteria((query, applicationShutdown) -> query.startedAt(applicationShutdown.getStartedAt()), 2);
    }

    private interface ApplicationShutdownQueryBuilder {
        ApplicationShutdownQuery build(ApplicationShutdownQuery applicationShutdownQuery, ApplicationShutdown applicationShutdown);
    }

    private void testQueryByCriteria(ApplicationShutdownQueryBuilder applicationShutdownQueryBuilder, int resultCount) {
        addApplicationShutdown(List.of(APPLICATION_SHUTDOWN, APPLICATION_SHUTDOWN_2));
        assertEquals(resultCount, applicationShutdownQueryBuilder.build(applicationShutdownService.createQuery(), APPLICATION_SHUTDOWN)
                                                                 .list()
                                                                 .size());
        assertEquals(resultCount, applicationShutdownQueryBuilder.build(applicationShutdownService.createQuery(), APPLICATION_SHUTDOWN)
                                                                 .delete());
        assertApplicationShutdownExists(APPLICATION_SHUTDOWN_2.getId());
    }

    private void assertApplicationShutdownExists(String id) {
        // If does not exist, will throw NoResultException
        applicationShutdownService.createQuery()
                                  .id(id);
    }

    private void addApplicationShutdown(List<ApplicationShutdown> applicationShutdowns) {
        applicationShutdowns.forEach(applicationShutdownService::add);
    }

    private ApplicationShutdown createApplicationShutdownInstance(String instanceId, int index) {
        return ImmutableApplicationShutdown.builder()
                                           .id(instanceId)
                                           .applicationId(APPLICATION_ID)
                                           .applicationInstanceIndex(index)
                                           .startedAt(Date.from(Instant.now()))
                                           .status(ApplicationShutdown.Status.FINISHED)
                                           .build();
    }

    private ApplicationShutdownService createApplicationShutdownService() {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("TestDefault");
        ApplicationShutdownService.ApplicationShutdownMapper applicationShutdownMapper = new ApplicationShutdownService.ApplicationShutdownMapper();
        return new ApplicationShutdownService(entityManagerFactory,
                                              applicationShutdownMapper);
    }
}
