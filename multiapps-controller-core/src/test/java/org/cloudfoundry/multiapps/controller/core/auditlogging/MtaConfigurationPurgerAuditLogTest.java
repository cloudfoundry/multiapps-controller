package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableBackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class MtaConfigurationPurgerAuditLogTest {

    private static final String SPACE_GUID = "space-guid";
    private static final String MTA_ID = "my-mta";
    private static final String APP_NAME = "app";
    private static final long SUBSCRIPTION_ID = 17L;

    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private MtaConfigurationPurgerAuditLog purgerAuditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        purgerAuditLog = new MtaConfigurationPurgerAuditLog(auditLoggingFacade);
    }

    @Test
    void testLogDeleteSubscriptionWithSubscriptionEmitsConfigurationDelete() {
        ConfigurationSubscription subscription = new ConfigurationSubscription(SUBSCRIPTION_ID, MTA_ID, SPACE_GUID, APP_NAME, null, null,
                                                                                null, null, null);

        purgerAuditLog.logDeleteSubscription(SPACE_GUID, subscription);

        AuditLogConfiguration captured = captureDeleteConfig();
        Assertions.assertEquals(SPACE_GUID, captured.getSpaceId());
        Assertions.assertTrue(containsParameter(captured, "applicationId", APP_NAME));
        Assertions.assertTrue(containsParameter(captured, "mtaId", MTA_ID));
        Assertions.assertTrue(containsParameter(captured, "subscriptionId", String.valueOf(SUBSCRIPTION_ID)));
    }

    @Test
    void testLogDeleteSubscriptionWithoutSubscriptionEmitsConfigurationDelete() {
        purgerAuditLog.logDeleteSubscription(SPACE_GUID);

        AuditLogConfiguration captured = captureDeleteConfig();
        Assertions.assertEquals(SPACE_GUID, captured.getSpaceId());
        Assertions.assertFalse(containsParameter(captured, "subscriptionId", String.valueOf(SUBSCRIPTION_ID)));
    }

    @Test
    void testLogDeleteEntryWithEntryEmitsAllProviderIdentifiers() {
        CloudTarget targetSpace = new CloudTarget("my-org", "my-space");
        ConfigurationEntry entry = new ConfigurationEntry("nid", "pid", Version.parseVersion("1.0.0"), "ns", targetSpace,
                                                          "content", java.util.List.of(), SPACE_GUID, "content-id");

        purgerAuditLog.logDeleteEntry(SPACE_GUID, entry);

        AuditLogConfiguration captured = captureDeleteConfig();
        Assertions.assertTrue(containsParameter(captured, "providerId", "pid"));
        Assertions.assertTrue(containsParameter(captured, "providerNid", "nid"));
        Assertions.assertTrue(containsParameter(captured, "providerVersion", "1.0.0"));
        Assertions.assertTrue(containsParameter(captured, "providerNamespace", "ns"));
        Assertions.assertTrue(containsParameter(captured, "providerTarget", "my-org/my-space"));
        Assertions.assertTrue(containsParameter(captured, "providerContent", "content"));
        Assertions.assertTrue(containsParameter(captured, "providerContentId", "content-id"));
    }

    @Test
    void testLogDeleteEntryWithoutEntryEmitsConfigurationDelete() {
        purgerAuditLog.logDeleteEntry(SPACE_GUID);

        AuditLogConfiguration captured = captureDeleteConfig();
        Assertions.assertEquals(SPACE_GUID, captured.getSpaceId());
        Assertions.assertFalse(containsParameter(captured, "providerId", "pid"));
    }

    @Test
    void testLogDeleteOperationEmitsOperationIdentifiers() {
        Operation operation = ImmutableOperation.builder()
                                                .processId("p-1")
                                                .processType(ProcessType.DEPLOY)
                                                .user("alice")
                                                .state(Operation.State.FINISHED)
                                                .build();

        purgerAuditLog.logDeleteOperation(SPACE_GUID, operation);

        AuditLogConfiguration captured = captureDeleteConfig();
        Assertions.assertEquals("alice", captured.getUserId());
        Assertions.assertEquals(SPACE_GUID, captured.getSpaceId());
        Assertions.assertTrue(containsParameter(captured, "processType", ProcessType.DEPLOY.toString()));
        Assertions.assertTrue(containsParameter(captured, "state", Operation.State.FINISHED.toString()));
    }

    @Test
    void testLogDeleteBackupDescriptorEmitsMtaAndTimestampIdentifiers() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        BackupDescriptor descriptor = ImmutableBackupDescriptor.builder()
                                                               .mtaId(MTA_ID)
                                                               .mtaVersion("1.2.3")
                                                               .spaceId(SPACE_GUID)
                                                               .timestamp(timestamp)
                                                               .descriptor(DeploymentDescriptor.createV3())
                                                               .build();

        purgerAuditLog.logDeleteBackupDescriptor(SPACE_GUID, descriptor);

        AuditLogConfiguration captured = captureDeleteConfig();
        Assertions.assertEquals(SPACE_GUID, captured.getSpaceId());
        Assertions.assertTrue(containsParameter(captured, "mtaId", MTA_ID));
        Assertions.assertTrue(containsParameter(captured, "storedAt", timestamp.toString()));
    }

    private AuditLogConfiguration captureDeleteConfig() {
        ArgumentCaptor<AuditLogConfiguration> configCaptor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        ArgumentCaptor<ConfigurationChangeActions> actionCaptor = ArgumentCaptor.forClass(ConfigurationChangeActions.class);
        Mockito.verify(auditLoggingFacade)
               .logConfigurationChangeAuditLog(configCaptor.capture(), actionCaptor.capture());
        Assertions.assertEquals(ConfigurationChangeActions.CONFIGURATION_DELETE, actionCaptor.getValue());
        return configCaptor.getValue();
    }

    private boolean containsParameter(AuditLogConfiguration configuration, String key, String value) {
        return configuration.getConfigurationIdentifiers()
                            .stream()
                            .anyMatch(identifier -> key.equals(identifier.getIdentifierName())
                                && value.equals(identifier.getIdentifierValue()));
    }

}
