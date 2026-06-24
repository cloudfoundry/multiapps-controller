package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class ConfigurationSubscriptionServiceAuditLogTest {

    private static final String USERNAME = "alice";
    private static final String SPACE_ID = "space-guid";
    private static final String MTA_ID = "my-mta";
    private static final String APP_NAME = "app";
    private static final long SUBSCRIPTION_ID = 17L;

    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private ConfigurationSubscriptionServiceAuditLog auditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        auditLog = new ConfigurationSubscriptionServiceAuditLog(auditLoggingFacade);
    }

    @Test
    void testLogAddConfigurationSubscriptionEmitsConfigurationCreate() {
        ConfigurationSubscription subscription = new ConfigurationSubscription(SUBSCRIPTION_ID, MTA_ID, SPACE_ID, APP_NAME, null, null,
                                                                               null, null, null);

        auditLog.logAddConfigurationSubscription(USERNAME, SPACE_ID, subscription);

        ArgumentCaptor<AuditLogConfiguration> configCaptor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        ArgumentCaptor<ConfigurationChangeActions> actionCaptor = ArgumentCaptor.forClass(ConfigurationChangeActions.class);
        Mockito.verify(auditLoggingFacade)
               .logConfigurationChangeAuditLog(configCaptor.capture(), actionCaptor.capture());
        Assertions.assertEquals(ConfigurationChangeActions.CONFIGURATION_CREATE, actionCaptor.getValue());

        AuditLogConfiguration captured = configCaptor.getValue();
        Assertions.assertEquals(USERNAME, captured.getUserId());
        Assertions.assertEquals(SPACE_ID, captured.getSpaceId());
        Assertions.assertTrue(containsParameter(captured, "applicationId", APP_NAME));
        Assertions.assertTrue(containsParameter(captured, "mtaId", MTA_ID));
        Assertions.assertTrue(containsParameter(captured, "subscriptionId", String.valueOf(SUBSCRIPTION_ID)));
    }

    @Test
    void testLogUpdateConfigurationSubscriptionDelegatesToFourArgFacadeMethod() {
        ConfigurationSubscription oldSub = new ConfigurationSubscription(1L, MTA_ID, SPACE_ID, "old-app", null, null, null, null, null);
        ConfigurationSubscription newSub = new ConfigurationSubscription(2L, MTA_ID, SPACE_ID, "new-app", null, null, null, null, null);

        auditLog.logUpdateConfigurationSubscription(USERNAME, SPACE_ID, oldSub, newSub);

        Mockito.verify(auditLoggingFacade)
               .logConfigurationChangeAuditLog(Mockito.any(AuditLogConfiguration.class),
                                               Mockito.eq(ConfigurationChangeActions.CONFIGURATION_UPDATE), Mockito.eq(oldSub),
                                               Mockito.eq(newSub));
    }

    private boolean containsParameter(AuditLogConfiguration configuration, String key, String value) {
        return configuration.getConfigurationIdentifiers()
                            .stream()
                            .anyMatch(identifier -> key.equals(identifier.getIdentifierName())
                                && value.equals(identifier.getIdentifierValue()));
    }

}
