package org.cloudfoundry.multiapps.controller.core.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableLifecycle;
import org.cloudfoundry.multiapps.controller.client.facade.domain.LifecycleType;
import org.cloudfoundry.multiapps.controller.client.facade.rest.CloudSpaceClient;
import org.cloudfoundry.multiapps.controller.core.auditlogging.CloudLoggingServiceConfigurationAuditLog;
import org.cloudfoundry.multiapps.controller.core.auditlogging.MtaConfigurationPurgerAuditLog;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataValidator;
import org.cloudfoundry.multiapps.controller.core.util.ConfigurationEntriesUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationEntryQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationSubscriptionQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.Query;
import org.cloudfoundry.multiapps.controller.persistence.services.CloudLoggingServiceConfigurationService;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MtaConfigurationPurgerTest {

    private static final int ENTRY_ID_TO_REMOVE = 0;
    private static final int ENTRY_ID_TO_KEEP_1 = 1;
    private static final int ENTRY_ID_TO_KEEP_2 = 2;
    private static final int SUBSCRIPTION_ID_TO_REMOVE = 2;
    private static final int SUBSCRIPTION_ID_TO_KEEP = 3;
    private static final String APPLICATION_NAME_TO_KEEP = "app-to-keep";
    private static final String APPLICATION_NAME_TO_REMOVE = "app-to-remove";
    private final static String TARGET_SPACE = "space";
    private final static String TARGET_ORG = "org";
    private final ConfigurationEntry ENTRY_TO_DELETE = createEntry(ENTRY_ID_TO_REMOVE, "remove:true");
    private final ConfigurationSubscription SUBSCRIPTION_TO_DELETE = createSubscription(SUBSCRIPTION_ID_TO_REMOVE,
                                                                                        APPLICATION_NAME_TO_REMOVE);
    private final List<Query<?, ?>> queriesToVerifyDeleteCallOn = new ArrayList<>();
    private final List<Query<?, ?>> queriesToVerifyNoDeleteCallOn = new ArrayList<>();
    @Mock
    CloudControllerClient client;
    @Mock
    CloudSpaceClient spaceClient;
    @Mock
    ConfigurationEntryService configurationEntryService;
    @Mock(answer = Answers.RETURNS_SELF)
    ConfigurationEntryQuery configurationEntryQuery;
    @Mock
    ConfigurationSubscriptionService configurationSubscriptionService;
    @Mock(answer = Answers.RETURNS_SELF)
    ConfigurationSubscriptionQuery configurationSubscriptionQuery;
    @Mock
    MtaConfigurationPurgerAuditLog mtaConfigurationPurgerAuditLog;
    @Mock
    CloudLoggingServiceConfigurationService cloudLoggingServiceConfigurationService;
    @Mock
    CloudLoggingServiceConfigurationAuditLog cloudLoggingServiceConfigurationAuditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        initApplicationsMock();
        initConfigurationEntriesMock();
        initConfigurationSubscriptionsMock();
    }

    @Test
    void testPurge() {
        MtaConfigurationPurger purger = new MtaConfigurationPurger(client, spaceClient, configurationEntryService,
                                                                   configurationSubscriptionService,
                                                                   new MtaMetadataParser(new MtaMetadataValidator()),
                                                                   mtaConfigurationPurgerAuditLog, cloudLoggingServiceConfigurationService,
                                                                   cloudLoggingServiceConfigurationAuditLog);
        purger.purge("org", "space");
        verifyConfigurationEntriesDeleted();
        verifyConfigurationEntriesNotDeleted();
    }

    private void verifyConfigurationEntriesDeleted() {
        for (Query<?, ?> queryToExecuteDeleteOn : queriesToVerifyDeleteCallOn) {
            Mockito.verify(queryToExecuteDeleteOn)
                   .delete();
        }
    }

    private void verifyConfigurationEntriesNotDeleted() {
        for (Query<?, ?> queryNotToExecuteDeleteOn : queriesToVerifyNoDeleteCallOn) {
            Mockito.verify(queryNotToExecuteDeleteOn, Mockito.never())
                   .delete();
        }
    }

    private void initApplicationsMock() {
        List<CloudApplication> applications = new ArrayList<>();
        applications.add(createApplication(APPLICATION_NAME_TO_KEEP));
        applications.add(createApplication("app-2"));
        Mockito.when(client.getApplications())
               .thenReturn(applications);
    }

    private void initConfigurationEntriesMock() {
        when(configurationEntryService.createQuery()).thenReturn(configurationEntryQuery);
        doReturn(getConfigurationEntries()).when(configurationEntryQuery)
                                           .list();
    }

    private List<ConfigurationEntry> getConfigurationEntries() {
        return List.of(ENTRY_TO_DELETE, createEntry(ENTRY_ID_TO_KEEP_1, "anatz:dependency-1"),
                       createEntry(ENTRY_ID_TO_KEEP_2, "anatz:dependency-2"));
    }

    private void initConfigurationSubscriptionsMock() {
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        doReturn(getConfigurationSubscriptions()).when(configurationSubscriptionQuery)
                                                 .list();
    }

    private List<ConfigurationSubscription> getConfigurationSubscriptions() {
        return List.of(SUBSCRIPTION_TO_DELETE, createSubscription(SUBSCRIPTION_ID_TO_KEEP, APPLICATION_NAME_TO_KEEP));
    }

    private CloudApplication createApplication(String applicationName) {
        return ImmutableCloudApplication.builder()
                                        .name(applicationName)
                                        .metadata(CloudMetadata.defaultMetadata())
                                        .state(CloudApplication.State.STOPPED)
                                        .lifecycle(ImmutableLifecycle.builder()
                                                                     .type(LifecycleType.DOCKER)
                                                                     .build())
                                        .build();
    }

    private ConfigurationSubscription createSubscription(int id, String applicationName) {
        return new ConfigurationSubscription(id, null, null, applicationName, null, null, null, null, null);
    }

    private ConfigurationEntry createEntry(int id, String providerId) {
        return new ConfigurationEntry(id, ConfigurationEntriesUtil.PROVIDER_NID, providerId, Version.parseVersion("1.0.0"), null,
                                      new CloudTarget(TARGET_ORG, TARGET_SPACE), null, null, null, null);
    }

    @Test
    void testPurgeCloudLoggingServiceConfigurations_deletesAllConfigurationsInSpace() {
        String spaceId = "00000000-0000-0000-0000-000000000001";
        LoggingConfiguration config1 = createLoggingConfiguration("id-1", spaceId, "mta-1");
        LoggingConfiguration config2 = createLoggingConfiguration("id-2", spaceId, "mta-2");
        when(cloudLoggingServiceConfigurationService.getAllCloudLoggingServiceConfigurationsFromSpace(spaceId)).thenReturn(List.of(config1,
                                                                                                                                  config2));
        when(spaceClient.getSpace(TARGET_ORG, TARGET_SPACE)).thenReturn(createCloudSpace(spaceId));

        MtaConfigurationPurger purger = createPurger();
        purger.purge(TARGET_ORG, TARGET_SPACE);

        verify(cloudLoggingServiceConfigurationService).deleteCloudLoggingServiceConfiguration("id-1");
        verify(cloudLoggingServiceConfigurationService).deleteCloudLoggingServiceConfiguration("id-2");
        verify(cloudLoggingServiceConfigurationAuditLog).logDeleteLoggingConfiguration("", spaceId, config1);
        verify(cloudLoggingServiceConfigurationAuditLog).logDeleteLoggingConfiguration("", spaceId, config2);
    }

    @Test
    void testPurgeCloudLoggingServiceConfigurations_doesNothingWhenNoConfigurationsExist() {
        String spaceId = "00000000-0000-0000-0000-000000000002";
        when(cloudLoggingServiceConfigurationService.getAllCloudLoggingServiceConfigurationsFromSpace(spaceId)).thenReturn(List.of());
        when(spaceClient.getSpace(TARGET_ORG, TARGET_SPACE)).thenReturn(createCloudSpace(spaceId));

        MtaConfigurationPurger purger = createPurger();
        purger.purge(TARGET_ORG, TARGET_SPACE);

        verify(cloudLoggingServiceConfigurationService, never()).deleteCloudLoggingServiceConfiguration(Mockito.anyString());
        verify(cloudLoggingServiceConfigurationAuditLog, never()).logDeleteLoggingConfiguration(Mockito.anyString(), Mockito.anyString(),
                                                                                                Mockito.any());
    }

    @Test
    void testPurgeCloudLoggingServiceConfigurations_deletesSingleConfiguration() {
        String spaceId = "00000000-0000-0000-0000-000000000003";
        LoggingConfiguration config = createLoggingConfiguration("id-1", spaceId, "mta-1");
        when(cloudLoggingServiceConfigurationService.getAllCloudLoggingServiceConfigurationsFromSpace(spaceId)).thenReturn(List.of(config));
        when(spaceClient.getSpace(TARGET_ORG, TARGET_SPACE)).thenReturn(createCloudSpace(spaceId));

        MtaConfigurationPurger purger = createPurger();
        purger.purge(TARGET_ORG, TARGET_SPACE);

        verify(cloudLoggingServiceConfigurationService).deleteCloudLoggingServiceConfiguration("id-1");
        verify(cloudLoggingServiceConfigurationAuditLog).logDeleteLoggingConfiguration("", spaceId, config);
    }

    private MtaConfigurationPurger createPurger() {
        return new MtaConfigurationPurger(client, spaceClient, configurationEntryService, configurationSubscriptionService,
                                          new MtaMetadataParser(new MtaMetadataValidator()), mtaConfigurationPurgerAuditLog,
                                          cloudLoggingServiceConfigurationService, cloudLoggingServiceConfigurationAuditLog);
    }

    private LoggingConfiguration createLoggingConfiguration(String id, String spaceId, String mtaId) {
        return ImmutableLoggingConfiguration.builder()
                                            .id(id)
                                            .mtaSpaceId(spaceId)
                                            .mtaId(mtaId)
                                            .build();
    }

    private CloudSpace createCloudSpace(String spaceId) {
        return ImmutableCloudSpace.builder()
                                  .metadata(ImmutableCloudMetadata.builder()
                                                                   .guid(UUID.fromString(spaceId))
                                                                   .build())
                                  .name(TARGET_SPACE)
                                  .build();
    }

}
