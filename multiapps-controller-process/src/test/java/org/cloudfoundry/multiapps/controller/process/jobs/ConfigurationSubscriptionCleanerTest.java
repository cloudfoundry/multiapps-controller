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
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationSubscriptionQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;

class ConfigurationSubscriptionCleanerTest {

    private static final UUID EXISTING_SPACE_1 = UUID.randomUUID();
    private static final UUID EXISTING_SPACE_2 = UUID.randomUUID();
    private static final UUID NON_EXISTING_SPACE = UUID.randomUUID();

    @Mock
    private AuditLoggingFacade auditLoggingFacade;
    @Mock
    private ApplicationConfiguration configuration;
    @Mock
    private CloudControllerClient clientMock;
    @Mock
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Mock
    private ConfigurationSubscriptionQuery query;

    private ConfigurationSubscriptionCleaner cleaner;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        cleaner = new ConfigurationSubscriptionCleaner(configuration, configurationSubscriptionService) {
            @Override
            protected void initCloudControllerClient() {
                super.client = clientMock;
            }
        };
        AuditLoggingProvider.setFacade(auditLoggingFacade);
    }

    @Test
    void testDeleteOrphanedConfigurationSubscriptions() {
        List<ConfigurationSubscription> configurationSubscriptions = generateConfigurationSubscriptions();
        prepareConfigurationSubscriptionService(configurationSubscriptions);
        prepareClient();

        cleaner.execute(null);

        verifyDeleteCall();
    }

    @Test
    void testSingleExecution() {
        prepareConfigurationSubscriptionService(Collections.emptyList());

        cleaner.execute(null);
        cleaner.execute(null);

        verify(query, only()).list();
    }

    private List<ConfigurationSubscription> generateConfigurationSubscriptions() {
        List<ConfigurationSubscription> configurationSubscriptions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            configurationSubscriptions.add(new ConfigurationSubscription(i,
                                                                         "mtaId" + i,
                                                                         NON_EXISTING_SPACE.toString(),
                                                                         "app" + i,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null));
        }
        for (int i = 10; i < 15; i++) {
            configurationSubscriptions.add(new ConfigurationSubscription(i,
                                                                         "mtaId" + i,
                                                                         EXISTING_SPACE_1.toString(),
                                                                         "app" + i,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null));
        }
        for (int i = 15; i < 20; i++) {
            configurationSubscriptions.add(new ConfigurationSubscription(i,
                                                                         "mtaId" + i,
                                                                         EXISTING_SPACE_2.toString(),
                                                                         "app" + i,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null));
        }
        return configurationSubscriptions;
    }

    private void prepareConfigurationSubscriptionService(List<ConfigurationSubscription> configurationSubscriptions) {
        when(query.list()).thenReturn(configurationSubscriptions);
        when(configurationSubscriptionService.createQuery()).thenReturn(query);
    }

    private void prepareClient() {
        when(clientMock.getSpace(eq(NON_EXISTING_SPACE))).thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        when(clientMock.getSpace(eq(EXISTING_SPACE_2))).thenThrow(new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private void verifyDeleteCall() {
        verify(query).deleteAll(NON_EXISTING_SPACE.toString());
    }
}
