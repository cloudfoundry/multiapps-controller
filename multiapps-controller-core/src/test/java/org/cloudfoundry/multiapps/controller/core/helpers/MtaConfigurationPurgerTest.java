package org.cloudfoundry.multiapps.controller.core.helpers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import com.sap.cloudfoundry.client.facade.rest.CloudSpaceClient;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.auditlogging.impl.AuditLoggingFacadeSLImpl;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataValidator;
import org.cloudfoundry.multiapps.controller.core.util.ConfigurationEntriesUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationEntryQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationSubscriptionQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.Query;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.LifecycleType;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableLifecycle;

class MtaConfigurationPurgerTest {

    private static final int ENTRY_ID_TO_REMOVE = 0;
    private static final int ENTRY_ID_TO_KEEP_1 = 1;
    private static final int ENTRY_ID_TO_KEEP_2 = 2;
    private static final int SUBSCRIPTION_ID_TO_REMOVE = 2;
    private static final int SUBSCRIPTION_ID_TO_KEEP = 3;
    private static final String APPLICATION_NAME_TO_KEEP = "app-to-keep";
    private static final String APPLICATION_NAME_TO_REMOVE = "app-to-remove";
    private final ConfigurationEntry ENTRY_TO_DELETE = createEntry(ENTRY_ID_TO_REMOVE, "remove:true");
    private final ConfigurationSubscription SUBSCRIPTION_TO_DELETE = createSubscription(SUBSCRIPTION_ID_TO_REMOVE,
                                                                                        APPLICATION_NAME_TO_REMOVE);

    private final static String TARGET_SPACE = "space";
    private final static String TARGET_ORG = "org";

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
    AuditLoggingFacadeSLImpl auditLoggingFacade;

    private final List<Query<?, ?>> queriesToVerifyDeleteCallOn = new ArrayList<>();
    private final List<Query<?, ?>> queriesToVerifyNoDeleteCallOn = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        AuditLoggingProvider.setFacade(auditLoggingFacade);
        initApplicationsMock();
        initConfigurationEntriesMock();
        initConfigurationSubscriptionsMock();
    }

    @Test
    void testPurge() {
        MtaConfigurationPurger purger = new MtaConfigurationPurger(client, spaceClient,
                                                                   configurationEntryService,
                                                                   configurationSubscriptionService,
                                                                   new MtaMetadataParser(new MtaMetadataValidator()));
        purger.purge("org", "space");
        verifyConfigurationEntriesDeleted();
        verifyConfigurationEntriesNotDeleted();
        Mockito.verify(auditLoggingFacade)
               .logConfigDelete(ENTRY_TO_DELETE);
        Mockito.verify(auditLoggingFacade)
               .logConfigDelete(SUBSCRIPTION_TO_DELETE);
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
        return new ConfigurationEntry(id,
                                      ConfigurationEntriesUtil.PROVIDER_NID,
                                      providerId,
                                      Version.parseVersion("1.0.0"),
                                      null,
                                      new CloudTarget(TARGET_ORG, TARGET_SPACE),
                                      null,
                                      null,
                                      null,
                                      null);
    }

}
