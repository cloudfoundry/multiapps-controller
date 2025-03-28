package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.util.Strings;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;

public class MtaConfigurationPurgerAuditLog {

    private static final String APPLICATION_ID_PROPERTY_NAME = "applicationId";
    private static final String MTA_ID_PROPERTY_NAME = "mtaId";
    private static final String SUBSCRIPTION_ID_PROPERTY_NAME = "subscriptionId";
    private static final String PROVIDER_ID_PROPERTY_NAME = "providerId";
    private static final String PROVIDER_NID_PROPERTY_NAME = "providerNid";
    private static final String PROVIDER_VERSION_PROPERTY_NAME = "providerVersion";
    private static final String PROVIDER_NAMESPACE_PROPERTY_NAME = "providerNamespace";
    private static final String PROVIDER_TARGET_PROPERTY_NAME = "providerTarget";
    private static final String PROVIDER_CONTENT_PROPERTY_NAME = "providerContent";
    private static final String PROVIDER_CONTENT_ID_PROPERTY_NAME = "providerContentId";
    private static final String PROCESS_TYPE_PROPERTY_NAME = "processType";
    private static final String ENDED_AT_PROPERTY_NAME = "endedAt";
    private static final String STARTED_AT_PROPERTY_NAME = "startedAt";
    private static final String STATE_PROPERTY_NAME = "state";
    private static final String ERROR_TYPE_PROPERTY_NAME = "errorType";
    private static final String STORED_AT_PROPERTY_NAME = "storedAt";

    private final AuditLoggingFacade auditLoggingFacade;

    public MtaConfigurationPurgerAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void logDeleteSubscription(String spaceGuid, ConfigurationSubscription subscription) {
        String performedAction = MessageFormat.format(Messages.DELETE_SUBSCRIPTION_AUDIT_LOG_MESSAGE, spaceGuid);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(Strings.EMPTY,
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.SUBSCRIPTION_DELETE_AUDIT_LOG_CONFIG,
                                                                                    createAuditLogDeleteSubscriptionConfigurationIdentifier(
                                                                                        subscription)),
                                                          ConfigurationChangeActions.CONFIGURATION_DELETE);
    }

    public void logDeleteSubscription(String spaceGuid) {
        String performedAction = MessageFormat.format(Messages.DELETE_SUBSCRIPTION_AUDIT_LOG_MESSAGE, spaceGuid);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(Strings.EMPTY,
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.SUBSCRIPTION_DELETE_AUDIT_LOG_CONFIG),
                                                          ConfigurationChangeActions.CONFIGURATION_DELETE);
    }

    public void logDeleteEntry(String spaceGuid, ConfigurationEntry entry) {
        String performedAction = MessageFormat.format(Messages.DELETE_ENTRY_AUDIT_LOG_MESSAGE, spaceGuid);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(Strings.EMPTY,
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.ENTRY_DELETE_AUDIT_LOG_CONFIG,
                                                                                    createAuditLogDeleteEntryConfigurationIdentifier(
                                                                                        entry)),
                                                          ConfigurationChangeActions.CONFIGURATION_DELETE);
    }

    public void logDeleteEntry(String spaceGuid) {
        String performedAction = MessageFormat.format(Messages.DELETE_ENTRY_AUDIT_LOG_MESSAGE, spaceGuid);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(Strings.EMPTY,
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.ENTRY_DELETE_AUDIT_LOG_CONFIG),
                                                          ConfigurationChangeActions.CONFIGURATION_DELETE);
    }

    public void logDeleteOperation(String spaceGuid, Operation operation) {
        String performedAction = MessageFormat.format(Messages.DELETE_OPERATION_AUDIT_LOG_MESSAGE, spaceGuid);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(operation.getUser(),
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.OPERATION_DELETE_AUDIT_LOG_CONFIG,
                                                                                    createAuditLogDeleteOperationConfigurationIdentifier(
                                                                                        operation)),
                                                          ConfigurationChangeActions.CONFIGURATION_DELETE);
    }

    public void logDeleteBackupDescriptor(String spaceGuid, BackupDescriptor backupDescriptor) {
        String performedAction = MessageFormat.format(Messages.DELETE_BACKUP_DESCRIPTOR_AUDIT_LOG_MESSAGE, spaceGuid);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(Strings.EMPTY,
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.MTA_DESCRIPTOR_DELETE_AUDIT_LOG_CONFIG,
                                                                                    createAuditLogDeleteMtaBackupDescriptorIdentifier(
                                                                                        backupDescriptor)),
                                                          ConfigurationChangeActions.CONFIGURATION_DELETE);
    }

    private Map<String, String> createAuditLogDeleteSubscriptionConfigurationIdentifier(ConfigurationSubscription subscription) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(APPLICATION_ID_PROPERTY_NAME, subscription.getAppName());
        identifiers.put(MTA_ID_PROPERTY_NAME, subscription.getMtaId());
        identifiers.put(SUBSCRIPTION_ID_PROPERTY_NAME, String.valueOf(subscription.getId()));

        return identifiers;
    }

    private Map<String, String> createAuditLogDeleteEntryConfigurationIdentifier(ConfigurationEntry entry) {
        Map<String, String> identifiers = new HashMap<>();
        String providerTarget = entry.getTargetSpace()
                                     .getOrganizationName()
            + "/" + entry.getTargetSpace()
                         .getSpaceName();

        identifiers.put(PROVIDER_ID_PROPERTY_NAME, entry.getProviderId());
        identifiers.put(PROVIDER_NID_PROPERTY_NAME, entry.getProviderNid());
        identifiers.put(PROVIDER_VERSION_PROPERTY_NAME, Objects.toString(entry.getProviderVersion()));
        identifiers.put(PROVIDER_NAMESPACE_PROPERTY_NAME, entry.getProviderNamespace());
        identifiers.put(PROVIDER_TARGET_PROPERTY_NAME, providerTarget);
        identifiers.put(PROVIDER_CONTENT_PROPERTY_NAME, entry.getContent());
        identifiers.put(PROVIDER_CONTENT_ID_PROPERTY_NAME, entry.getContentId());

        return identifiers;
    }

    private Map<String, String> createAuditLogDeleteOperationConfigurationIdentifier(Operation operation) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(PROCESS_TYPE_PROPERTY_NAME, Objects.toString(operation.getProcessType()));
        identifiers.put(ENDED_AT_PROPERTY_NAME, Objects.toString(operation.getEndedAt()));
        identifiers.put(STARTED_AT_PROPERTY_NAME, Objects.toString(operation.getStartedAt()));
        identifiers.put(STATE_PROPERTY_NAME, Objects.toString(operation.getState()));
        identifiers.put(ERROR_TYPE_PROPERTY_NAME, Objects.toString(operation.getErrorType()));

        return identifiers;
    }

    private Map<String, String> createAuditLogDeleteMtaBackupDescriptorIdentifier(BackupDescriptor backupDescriptor) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(MTA_ID_PROPERTY_NAME, backupDescriptor.getMtaId());
        identifiers.put(STORED_AT_PROPERTY_NAME, backupDescriptor.getTimestamp()
                                                                 .toString());

        return identifiers;
    }
}