package org.cloudfoundry.multiapps.controller.persistence.services.cloudlogging;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CloudLoggingServiceConfigurationServiceTest {

    private static final String SPACE_ID_1 = "space-id-1";
    private static final String SPACE_ID_2 = "space-id-2";
    private static final String MTA_SPACE_1 = "mta-space-1";
    private static final String MTA_ID_1 = "mta-id-1";
    private static final String MTA_ID_2 = "mta-id-2";
    private static final String ID_1 = "id-1";
    private static final String ID_2 = "id-2";

    private EntityManagerFactory entityManagerFactory;
    private CloudLoggingServiceConfigurationService service;

    @BeforeEach
    void setUp() {
        entityManagerFactory = Persistence.createEntityManagerFactory("TestDefault");
        service = new CloudLoggingServiceConfigurationService(entityManagerFactory);
    }

    @AfterEach
    void tearDown() {
        service.createQuery()
               .mtaSpaceId(SPACE_ID_1)
               .list()
               .forEach(config -> service.createQuery()
                                         .id(config.getId())
                                         .delete());
        service.createQuery()
               .mtaSpaceId(SPACE_ID_2)
               .list()
               .forEach(config -> service.createQuery()
                                         .id(config.getId())
                                         .delete());
        entityManagerFactory.close();
    }

    @Test
    void testStoreAndGetConfiguration() {
        LoggingConfiguration config = buildConfiguration(ID_1, SPACE_ID_1, MTA_SPACE_1, MTA_ID_1, "ns-1");
        service.add(config);

        LoggingConfiguration result = getConfiguration(MTA_SPACE_1, MTA_ID_1, "ns-1");

        assertNotNull(result);
        assertEquals(ID_1, result.getId());
        assertEquals(MTA_ID_1, result.getMtaId());
        assertEquals(MTA_SPACE_1, result.getMtaSpace());
        assertEquals(SPACE_ID_1, result.getMtaSpaceId());
        assertEquals(LogLevel.INFO, result.getLogLevel());
    }

    @Test
    void testGetConfiguration_withNullNamespace() {
        LoggingConfiguration config = buildConfiguration(ID_1, SPACE_ID_1, MTA_SPACE_1, MTA_ID_1, null);
        service.add(config);

        LoggingConfiguration result = getConfiguration(MTA_SPACE_1, MTA_ID_1, null);

        assertNotNull(result);
        assertEquals(ID_1, result.getId());
    }

    @Test
    void testGetConfiguration_returnsNullWhenNotFound() {
        LoggingConfiguration result = getConfiguration("nonexistent-space", "nonexistent-mta", "ns");

        assertNull(result);
    }

    @Test
    void testDeleteConfiguration() {
        LoggingConfiguration config = buildConfiguration(ID_1, SPACE_ID_1, MTA_SPACE_1, MTA_ID_1, "ns-2");
        service.add(config);

        service.createQuery()
               .id(ID_1)
               .delete();

        assertNull(getConfiguration(MTA_SPACE_1, MTA_ID_1, "ns-2"));
    }

    @Test
    void testDeleteConfiguration_nonExistentIdDoesNotThrow() {
        int deletedCount = service.createQuery()
                                  .id("nonexistent-id")
                                  .delete();

        assertEquals(0, deletedCount);
    }

    @Test
    void testUpdateConfiguration() {
        LoggingConfiguration config = buildConfiguration(ID_1, SPACE_ID_1, MTA_SPACE_1, MTA_ID_1, "ns-3");
        service.add(config);

        LoggingConfiguration updated = ImmutableLoggingConfiguration.builder()
                                                                    .from(config)
                                                                    .logLevel(LogLevel.ERROR)
                                                                    .serviceInstanceName("updated-instance")
                                                                    .build();
        service.update(config, updated);

        LoggingConfiguration result = getConfiguration(MTA_SPACE_1, MTA_ID_1, "ns-3");
        assertNotNull(result);
        assertEquals(LogLevel.ERROR, result.getLogLevel());
        assertEquals("updated-instance", result.getServiceInstanceName());
    }

    @Test
    void testUpdateConfiguration_withNullNamespace() {
        LoggingConfiguration config = buildConfiguration(ID_1, SPACE_ID_1, MTA_SPACE_1, MTA_ID_1, null);
        service.add(config);

        LoggingConfiguration updated = ImmutableLoggingConfiguration.builder()
                                                                    .from(config)
                                                                    .logLevel(LogLevel.WARN)
                                                                    .build();
        service.update(config, updated);

        LoggingConfiguration result = getConfiguration(MTA_SPACE_1, MTA_ID_1, null);
        assertNotNull(result);
        assertEquals(LogLevel.WARN, result.getLogLevel());
    }

    @Test
    void testGetAllConfigurationsFromSpace_returnsAllForSpace() {
        service.add(buildConfiguration(ID_1, SPACE_ID_1, MTA_SPACE_1, MTA_ID_1, "ns-4"));
        service.add(buildConfiguration(ID_2, SPACE_ID_1, MTA_SPACE_1, MTA_ID_2, "ns-5"));

        List<LoggingConfiguration> results = service.createQuery()
                                                    .mtaSpaceId(SPACE_ID_1)
                                                    .list();

        assertEquals(2, results.size());
    }

    @Test
    void testGetAllConfigurationsFromSpace_returnsEmptyListWhenNoneExist() {
        List<LoggingConfiguration> results = service.createQuery()
                                                    .mtaSpaceId("unknown-space")
                                                    .list();

        assertEquals(0, results.size());
    }

    @Test
    void testGetAllConfigurationsFromSpace_doesNotReturnConfigurationsFromOtherSpaces() {
        service.add(buildConfiguration(ID_1, SPACE_ID_1, MTA_SPACE_1, MTA_ID_1, "ns-6"));
        service.add(buildConfiguration(ID_2, SPACE_ID_2, "mta-space-2", MTA_ID_2, "ns-7"));

        List<LoggingConfiguration> results = service.createQuery()
                                                    .mtaSpaceId(SPACE_ID_1)
                                                    .list();

        assertEquals(1, results.size());
        assertEquals(ID_1, results.get(0)
                                  .getId());
    }

    private LoggingConfiguration getConfiguration(String mtaSpace, String mtaId, String namespace) {
        return service.createQuery()
                      .mtaSpace(mtaSpace)
                      .mtaId(mtaId)
                      .namespace(namespace)
                      .list()
                      .stream()
                      .findFirst()
                      .orElse(null);
    }

    private LoggingConfiguration buildConfiguration(String id, String mtaSpaceId, String mtaSpace, String mtaId, String namespace) {
        return ImmutableLoggingConfiguration.builder()
                                            .id(id)
                                            .mtaSpaceId(mtaSpaceId)
                                            .mtaSpace(mtaSpace)
                                            .mtaId(mtaId)
                                            .mtaOrg("mta-org")
                                            .targetSpace("target-space")
                                            .targetOrg("target-org")
                                            .serviceInstanceName("my-cls-instance")
                                            .serviceKeyName("my-cls-key")
                                            .logLevel(LogLevel.INFO)
                                            .isFailSafe(true)
                                            .namespace(namespace)
                                            .build();
    }
}
