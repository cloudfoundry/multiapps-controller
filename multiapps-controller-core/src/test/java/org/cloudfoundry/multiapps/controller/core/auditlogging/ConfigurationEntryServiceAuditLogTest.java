package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class ConfigurationEntryServiceAuditLogTest {

    private static final String USERNAME = "alice";
    private static final String SPACE_ID = "space-guid";

    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private ConfigurationEntryServiceAuditLog auditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        auditLog = new ConfigurationEntryServiceAuditLog(auditLoggingFacade);
    }

    @Test
    void testLogAddConfigurationEntryEmitsConfigurationCreate() {
        ConfigurationEntry entry = new ConfigurationEntry("nid", "pid", Version.parseVersion("1.2.3"), "ns",
                                                          new CloudTarget("the-org", "the-space"), "content", java.util.List.of(),
                                                          SPACE_ID, "content-id");

        auditLog.logAddConfigurationEntry(USERNAME, SPACE_ID, entry);

        ArgumentCaptor<AuditLogConfiguration> configCaptor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        ArgumentCaptor<ConfigurationChangeActions> actionCaptor = ArgumentCaptor.forClass(ConfigurationChangeActions.class);
        Mockito.verify(auditLoggingFacade)
               .logConfigurationChangeAuditLog(configCaptor.capture(), actionCaptor.capture());
        Assertions.assertEquals(ConfigurationChangeActions.CONFIGURATION_CREATE, actionCaptor.getValue());

        AuditLogConfiguration captured = configCaptor.getValue();
        Assertions.assertEquals(USERNAME, captured.getUserId());
        Assertions.assertEquals(SPACE_ID, captured.getSpaceId());
        Assertions.assertTrue(containsParameter(captured, "providerId", "pid"));
        Assertions.assertTrue(containsParameter(captured, "providerNid", "nid"));
        Assertions.assertTrue(containsParameter(captured, "providerVersion", "1.2.3"));
        Assertions.assertTrue(containsParameter(captured, "providerNamespace", "ns"));
        Assertions.assertTrue(containsParameter(captured, "providerTarget", "the-org/the-space"));
        Assertions.assertTrue(containsParameter(captured, "providerContent", "content"));
        Assertions.assertTrue(containsParameter(captured, "providerContentId", "content-id"));
    }

    @Test
    void testLogUpdateConfigurationEntryDelegatesToFourArgFacadeMethod() {
        ConfigurationEntry oldEntry = new ConfigurationEntry("old-id", Version.parseVersion("1.0.0"));
        ConfigurationEntry newEntry = new ConfigurationEntry("new-id", Version.parseVersion("2.0.0"));

        auditLog.logUpdateConfigurationEntry(USERNAME, SPACE_ID, oldEntry, newEntry);

        ArgumentCaptor<AuditLogConfiguration> configCaptor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logConfigurationChangeAuditLog(configCaptor.capture(), Mockito.eq(ConfigurationChangeActions.CONFIGURATION_UPDATE),
                                               Mockito.eq(oldEntry), Mockito.eq(newEntry));
        Assertions.assertEquals(USERNAME, configCaptor.getValue()
                                                      .getUserId());
        Assertions.assertEquals(SPACE_ID, configCaptor.getValue()
                                                      .getSpaceId());
    }

    private boolean containsParameter(AuditLogConfiguration configuration, String key, String value) {
        return configuration.getConfigurationIdentifiers()
                            .stream()
                            .anyMatch(identifier -> key.equals(identifier.getIdentifierName())
                                && value.equals(identifier.getIdentifierValue()));
    }

}
