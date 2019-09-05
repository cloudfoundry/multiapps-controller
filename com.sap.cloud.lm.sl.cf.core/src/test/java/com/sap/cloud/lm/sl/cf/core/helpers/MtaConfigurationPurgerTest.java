package com.sap.cloud.lm.sl.cf.core.helpers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.auditlogging.impl.AuditLoggingFacadeSLImpl;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationEntryQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationSubscriptionQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.Query;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.model.Version;

public class MtaConfigurationPurgerTest {

    private static final int ENTRY_ID_TO_REMOVE = 0;
    private static final int ENTRY_ID_TO_KEEP_1 = 1;
    private static final int ENTRY_ID_TO_KEEP_2 = 2;
    private static final int SUBSCRIPTION_ID_TO_REMOVE = 2;
    private static final int SUBSCRIPTION_ID_TO_KEEP = 3;
    private static final String APPLICATION_NAME_TO_KEEP = "app-to-keep";
    private static final String APPLICATION_NAME_TO_REMOVE = "app-to-remove";
    private static final String RESOURCE_LOCATION = "application-env-01.json";
    private final ConfigurationEntry ENTRY_TO_DELETE = createEntry(ENTRY_ID_TO_REMOVE, "remove:true");
    private final ConfigurationSubscription SUBSCRIPTION_TO_DELETE = createSubscription(SUBSCRIPTION_ID_TO_REMOVE,
                                                                                        APPLICATION_NAME_TO_REMOVE);

    private final static String TARGET_SPACE = "space";
    private final static String TARGET_ORG = "org";

    @Mock
    CloudControllerClient client;

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

    private List<Query<?, ?>> queriesToVerifyDeleteCallOn = new ArrayList<>();
    private List<Query<?, ?>> queriesToVerifyNoDeleteCallOn = new ArrayList<>();

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        AuditLoggingProvider.setFacade(auditLoggingFacade);
        initApplicationsMock();
        initConfigurationEntriesMock();
        initConfigurationSubscriptionsMock();
    }

    @Test
    public void testPurge() {
        MtaConfigurationPurger purger = new MtaConfigurationPurger(client, configurationEntryService, configurationSubscriptionService);
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

    private void initApplicationsMock() throws IOException {
        List<CloudApplication> applications = new ArrayList<>();
        applications.add(createApplication(APPLICATION_NAME_TO_KEEP, getApplicationEnvFromFile(RESOURCE_LOCATION)));
        applications.add(createApplication("app-2", new HashMap<>()));
        Mockito.when(client.getApplications())
               .thenReturn(applications);
    }

    private void initConfigurationEntriesMock() {
        when(configurationEntryService.createQuery()).thenReturn(configurationEntryQuery);
        doReturn(getConfigurationEntries()).when(configurationEntryQuery)
                                           .list();
    }

    private List<ConfigurationEntry> getConfigurationEntries() {
        return Arrays.asList(ENTRY_TO_DELETE, createEntry(ENTRY_ID_TO_KEEP_1, "anatz:dependency-1"),
                             createEntry(ENTRY_ID_TO_KEEP_2, "anatz:dependency-2"));
    }

    private void initConfigurationSubscriptionsMock() {
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        doReturn(getConfigurationSubscriptions()).when(configurationSubscriptionQuery)
                                                 .list();
    }

    private List<ConfigurationSubscription> getConfigurationSubscriptions() {
        return Arrays.asList(SUBSCRIPTION_TO_DELETE, createSubscription(SUBSCRIPTION_ID_TO_KEEP, APPLICATION_NAME_TO_KEEP));
    }

    private CloudApplication createApplication(String applicationName, Map<String, Object> env) {
        MapToEnvironmentConverter envConverter = new MapToEnvironmentConverter(false);
        return ImmutableCloudApplication.builder()
                                        .name(applicationName)
                                        .env(envConverter.asEnv(env))
                                        .build();
    }

    private Map<String, Object> getApplicationEnvFromFile(String path) throws IOException {
        String envJson = TestUtil.getResourceAsString(path, MtaConfigurationPurgerTest.class);
        return JsonUtil.convertJsonToMap(envJson);
    }

    private ConfigurationSubscription createSubscription(int id, String applicationName) {
        return new ConfigurationSubscription(id, null, null, applicationName, null, null, null);
    }

    private ConfigurationEntry createEntry(int id, String providerId) {
        return new ConfigurationEntry(id,
                                      ConfigurationEntriesUtil.PROVIDER_NID,
                                      providerId,
                                      Version.parseVersion("1.0.0"),
                                      new CloudTarget(TARGET_ORG, TARGET_SPACE),
                                      null,
                                      null,
                                      null);
    }

}
