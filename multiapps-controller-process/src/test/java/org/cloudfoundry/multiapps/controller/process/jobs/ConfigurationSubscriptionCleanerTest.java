package org.cloudfoundry.multiapps.controller.process.jobs;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingFacade;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.core.persistence.query.ConfigurationSubscriptionQuery;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class ConfigurationSubscriptionCleanerTest {

    private ConfigurationSubscriptionCleaner cleaner;
    private ApplicationConfiguration applicationConfiguration;
    private CloudControllerClient cfClient;
    private ConfigurationSubscriptionService configurationSubscriptionService;

    private static final UUID FIRST_EXISTING_SPACE_ID = UUID.randomUUID();
    private static final UUID SECOND_EXISTING_SPACE_ID = UUID.randomUUID();
    private static final UUID FIRST_NON_EXISTING_SPACE_ID = UUID.randomUUID();
    private static final UUID SECOND_NON_EXISTING_SPACE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        applicationConfiguration = Mockito.mock(ApplicationConfiguration.class);
        configurationSubscriptionService = Mockito.mock(ConfigurationSubscriptionService.class);
        cfClient = Mockito.mock(CloudControllerClient.class);
        cleaner = new ConfigurationSubscriptionCleaner(applicationConfiguration, configurationSubscriptionService);
        cleaner.cfClient = cfClient;
        AuditLoggingProvider.setFacade(Mockito.mock(AuditLoggingFacade.class));
    }

    @Test
    void testExecute() {
        ConfigurationSubscriptionQuery fakeQuery = Mockito.mock(ConfigurationSubscriptionQuery.class);
        Mockito.when(configurationSubscriptionService.createQuery())
               .thenReturn(fakeQuery);
        Mockito.when(fakeQuery.list())
               .thenReturn(getMockedConfigurationSubscriptions());
        Mockito.when(cfClient.getSpace(FIRST_NON_EXISTING_SPACE_ID))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        Mockito.when(cfClient.getSpace(SECOND_NON_EXISTING_SPACE_ID))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));

        cleaner.execute(null);
        Mockito.verify(fakeQuery, Mockito.times(1))
               .deleteAll(FIRST_NON_EXISTING_SPACE_ID.toString());
        Mockito.verify(fakeQuery, Mockito.times(1))
               .deleteAll(SECOND_NON_EXISTING_SPACE_ID.toString());
        Mockito.verify(fakeQuery, Mockito.never())
               .deleteAll(FIRST_EXISTING_SPACE_ID.toString());
        Mockito.verify(fakeQuery, Mockito.never())
               .deleteAll(SECOND_EXISTING_SPACE_ID.toString());
    }

    private List<ConfigurationSubscription> getMockedConfigurationSubscriptions() {
        List<ConfigurationSubscription> configurationSubscriptions = new ArrayList<>(20);
        for (int i = 0; i < 5; i++) {
            configurationSubscriptions.add(new ConfigurationSubscription(i,
                                                                         "mtaId" + i,
                                                                         FIRST_NON_EXISTING_SPACE_ID.toString(),
                                                                         "app" + i,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null));
        }
        for (int i = 5; i < 10; i++) {
            configurationSubscriptions.add(new ConfigurationSubscription(i,
                                                                         "mtaId" + i,
                                                                         SECOND_NON_EXISTING_SPACE_ID.toString(),
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
                                                                         FIRST_EXISTING_SPACE_ID.toString(),
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
                                                                         SECOND_EXISTING_SPACE_ID.toString(),
                                                                         "app" + i,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null));
        }
        return configurationSubscriptions;
    }
}
