package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingFacade;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.cf.OAuthClientFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationEntryQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.rest.CloudSpaceClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;

class ConfigurationEntriesCleanerTest {
    private static final UUID EXISTING_SPACE_1 = UUID.randomUUID();
    private static final UUID EXISTING_SPACE_2 = UUID.randomUUID();
    private static final UUID NON_EXISTING_SPACE = UUID.randomUUID();

    @Mock
    private AuditLoggingFacade auditLoggingFacade;
    @Mock
    private ApplicationConfiguration configuration;
    @Mock
    private CloudSpaceClient clientMock;
    @Mock
    private ConfigurationEntryService configurationEntryService;
    @Mock
    private OAuthClientFactory oAuthClientFactory;
    @Mock
    private ConfigurationEntryQuery query;

    private ConfigurationEntriesCleaner cleaner;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        cleaner = new ConfigurationEntriesCleaner(configuration, configurationEntryService, oAuthClientFactory) {
            @Override
            protected void initSpaceClient() {
                super.spaceClient = clientMock;
            }
        };
        AuditLoggingProvider.setFacade(auditLoggingFacade);
    }

    @Test
    void testDeleteOrphanedConfigurationSubscriptions() {
        List<ConfigurationEntry> configurationEntries = generateConfigurationEntries();
        prepareConfigurationEntryService(configurationEntries);
        prepareClient();

        cleaner.execute(null);

        verifyDeleteCall();
    }

    @Test
    void testSingleExecution() {
        prepareConfigurationEntryService(Collections.emptyList());

        cleaner.execute(null);
        cleaner.execute(null);

        verify(query, only()).list();
    }

    private List<ConfigurationEntry> generateConfigurationEntries() {
        List<ConfigurationEntry> configurationEntries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            configurationEntries.add(new ConfigurationEntry(i,
                                                            null,
                                                            "mtaId" + i,
                                                            null,
                                                            null,
                                                            null,
                                                            null,
                                                            null,
                                                            NON_EXISTING_SPACE.toString(),
                                                            null));
        }
        for (int i = 10; i < 15; i++) {
            configurationEntries.add(new ConfigurationEntry(i,
                                                            null,
                                                            "mtaId" + i,
                                                            null,
                                                            null,
                                                            null,
                                                            null,
                                                            null,
                                                            EXISTING_SPACE_1.toString(),
                                                            null));
        }
        for (int i = 15; i < 20; i++) {
            configurationEntries.add(new ConfigurationEntry(i,
                                                            null,
                                                            "mtaId" + i,
                                                            null,
                                                            null,
                                                            null,
                                                            null,
                                                            null,
                                                            EXISTING_SPACE_2.toString(),
                                                            null));
        }
        return configurationEntries;
    }

    private void prepareConfigurationEntryService(List<ConfigurationEntry> configurationEntries) {
        when(query.list()).thenReturn(configurationEntries);
        when(configurationEntryService.createQuery()).thenReturn(query);
    }

    private void prepareClient() {
        when(clientMock.getSpace(eq(NON_EXISTING_SPACE))).thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        when(clientMock.getSpace(eq(EXISTING_SPACE_2))).thenThrow(new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private void verifyDeleteCall() {
        verify(query).deleteAll(NON_EXISTING_SPACE.toString());
    }
}
