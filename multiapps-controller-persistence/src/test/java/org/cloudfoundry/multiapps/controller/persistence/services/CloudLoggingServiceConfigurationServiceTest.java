package org.cloudfoundry.multiapps.controller.persistence.services;

import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.test.TestDataSourceProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CloudLoggingServiceConfigurationServiceTest {

    private static final String LIQUIBASE_CHANGELOG_LOCATION = "org/cloudfoundry/multiapps/controller/persistence/db/changelog/db-changelog.xml";

    private static final String SPACE_ID_1 = "space-id-1";
    private static final String SPACE_ID_2 = "space-id-2";
    private static final String MTA_SPACE_1 = "mta-space-1";
    private static final String MTA_ID_1 = "mta-id-1";
    private static final String MTA_ID_2 = "mta-id-2";
    private static final String ID_1 = "id-1";
    private static final String ID_2 = "id-2";

    private CloudLoggingServiceConfigurationService service;

    @BeforeEach
    void setUp() throws Exception {
        DataSourceWithDialect dataSource = new DataSourceWithDialect(TestDataSourceProvider.getDataSource(LIQUIBASE_CHANGELOG_LOCATION));
        service = new CloudLoggingServiceConfigurationService(dataSource);
    }

    @AfterEach
    void tearDown() {
        service.getAllCloudLoggingServiceConfigurationsFromSpace(SPACE_ID_1)
               .forEach(config -> service.deleteCloudLoggingServiceConfiguration(config.getId()));
        service.getAllCloudLoggingServiceConfigurationsFromSpace(SPACE_ID_2)
               .forEach(config -> service.deleteCloudLoggingServiceConfiguration(config.getId()));
    }

    @Test
    void testStoreAndGetConfiguration() {
        LoggingConfiguration config = buildConfiguration(ID_1, SPACE_ID_1, MTA_SPACE_1, MTA_ID_1, "ns-1");
        service.storeCloudLoggingServiceConfiguration(config);

        LoggingConfiguration result = service.getCloudLoggingServiceConfiguration(MTA_SPACE_1, MTA_ID_1, "ns-1");

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
        service.storeCloudLoggingServiceConfiguration(config);

        LoggingConfiguration result = service.getCloudLoggingServiceConfiguration(MTA_SPACE_1, MTA_ID_1, null);

        assertNotNull(result);
        assertEquals(ID_1, result.getId());
    }

    @Test
    void testGetConfiguration_returnsNullWhenNotFound() {
        LoggingConfiguration result = service.getCloudLoggingServiceConfiguration("nonexistent-space", "nonexistent-mta", "ns");

        assertNull(result);
    }

    @Test
    void testDeleteConfiguration() {
        LoggingConfiguration config = buildConfiguration(ID_1, SPACE_ID_1, MTA_SPACE_1, MTA_ID_1, "ns-2");
        service.storeCloudLoggingServiceConfiguration(config);

        service.deleteCloudLoggingServiceConfiguration(ID_1);

        assertNull(service.getCloudLoggingServiceConfiguration(MTA_SPACE_1, MTA_ID_1, "ns-2"));
    }

    @Test
    void testDeleteConfiguration_nonExistentIdDoesNotThrow() {
        service.deleteCloudLoggingServiceConfiguration("nonexistent-id");
    }

    @Test
    void testUpdateConfiguration() {
        LoggingConfiguration config = buildConfiguration(ID_1, SPACE_ID_1, MTA_SPACE_1, MTA_ID_1, "ns-3");
        service.storeCloudLoggingServiceConfiguration(config);

        LoggingConfiguration updated = ImmutableLoggingConfiguration.builder()
                                                                    .from(config)
                                                                    .logLevel(LogLevel.ERROR)
                                                                    .serviceInstanceName("updated-instance")
                                                                    .build();
        service.updateCloudLoggingServiceConfiguration(updated);

        LoggingConfiguration result = service.getCloudLoggingServiceConfiguration(MTA_SPACE_1, MTA_ID_1, "ns-3");
        assertNotNull(result);
        assertEquals(LogLevel.ERROR, result.getLogLevel());
        assertEquals("updated-instance", result.getServiceInstanceName());
    }

    @Test
    void testUpdateConfiguration_withNullNamespace() {
        LoggingConfiguration config = buildConfiguration(ID_1, SPACE_ID_1, MTA_SPACE_1, MTA_ID_1, null);
        service.storeCloudLoggingServiceConfiguration(config);

        LoggingConfiguration updated = ImmutableLoggingConfiguration.builder()
                                                                    .from(config)
                                                                    .logLevel(LogLevel.WARN)
                                                                    .build();
        service.updateCloudLoggingServiceConfiguration(updated);

        LoggingConfiguration result = service.getCloudLoggingServiceConfiguration(MTA_SPACE_1, MTA_ID_1, null);
        assertNotNull(result);
        assertEquals(LogLevel.WARN, result.getLogLevel());
    }

    @Test
    void testGetAllConfigurationsFromSpace_returnsAllForSpace() {
        service.storeCloudLoggingServiceConfiguration(buildConfiguration(ID_1, SPACE_ID_1, MTA_SPACE_1, MTA_ID_1, "ns-4"));
        service.storeCloudLoggingServiceConfiguration(buildConfiguration(ID_2, SPACE_ID_1, MTA_SPACE_1, MTA_ID_2, "ns-5"));

        List<LoggingConfiguration> results = service.getAllCloudLoggingServiceConfigurationsFromSpace(SPACE_ID_1);

        assertEquals(2, results.size());
    }

    @Test
    void testGetAllConfigurationsFromSpace_returnsEmptyListWhenNoneExist() {
        List<LoggingConfiguration> results = service.getAllCloudLoggingServiceConfigurationsFromSpace("unknown-space");

        assertEquals(0, results.size());
    }

    @Test
    void testGetAllConfigurationsFromSpace_doesNotReturnConfigurationsFromOtherSpaces() {
        service.storeCloudLoggingServiceConfiguration(buildConfiguration(ID_1, SPACE_ID_1, MTA_SPACE_1, MTA_ID_1, "ns-6"));
        service.storeCloudLoggingServiceConfiguration(buildConfiguration(ID_2, SPACE_ID_2, "mta-space-2", MTA_ID_2, "ns-7"));

        List<LoggingConfiguration> results = service.getAllCloudLoggingServiceConfigurationsFromSpace(SPACE_ID_1);

        assertEquals(1, results.size());
        assertEquals(ID_1, results.get(0)
                                  .getId());
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
