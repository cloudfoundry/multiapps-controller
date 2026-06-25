package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;

public class CloudLoggingServiceConfigurationAuditLog {

    private static final String ID_PROPERTY_NAME = "id";
    private static final String MTA_ID_PROPERTY_NAME = "mtaId";
    private static final String MTA_SPACE_PROPERTY_NAME = "mtaSpace";
    private static final String MTA_SPACE_ID_PROPERTY_NAME = "mtaSpaceId";
    private static final String MTA_ORG_PROPERTY_NAME = "mtaOrg";
    private static final String NAMESPACE_PROPERTY_NAME = "namespace";
    private static final String TARGET_SPACE_PROPERTY_NAME = "targetSpace";
    private static final String TARGET_ORG_PROPERTY_NAME = "targetOrg";
    private static final String SERVICE_INSTANCE_NAME_PROPERTY_NAME = "serviceInstanceName";
    private static final String SERVICE_KEY_NAME_PROPERTY_NAME = "serviceKeyName";
    private static final String LOG_LEVEL_PROPERTY_NAME = "logLevel";
    private static final String IS_FAILSAFE_PROPERTY_NAME = "isFailSafe";

    private final AuditLoggingFacade auditLoggingFacade;

    public CloudLoggingServiceConfigurationAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void logCreateLoggingConfiguration(String username, String spaceId, LoggingConfiguration loggingConfiguration) {
        String performedAction = MessageFormat.format(Messages.LOGGING_CONFIGURATION_CREATE, spaceId);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username,
                                                                                    spaceId,
                                                                                    performedAction,
                                                                                    Messages.LOGGING_CONFIGURATION_CREATE_AUDIT_LOG_CONFIG,
                                                                                    buildIdentifiers(loggingConfiguration)),
                                                          ConfigurationChangeActions.CONFIGURATION_CREATE);
    }

    public void logUpdateLoggingConfiguration(String username, String spaceId, LoggingConfiguration newConfiguration) {
        String performedAction = MessageFormat.format(Messages.LOGGING_CONFIGURATION_UPDATE, spaceId);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username,
                                                                                    spaceId,
                                                                                    performedAction,
                                                                                    Messages.LOGGING_CONFIGURATION_UPDATE_AUDIT_LOG_CONFIG,
                                                                                    buildIdentifiers(newConfiguration)),
                                                          ConfigurationChangeActions.CONFIGURATION_UPDATE);
    }

    public void logDeleteLoggingConfiguration(String username, String spaceId, LoggingConfiguration loggingConfiguration) {
        String performedAction = MessageFormat.format(Messages.LOGGING_CONFIGURATION_DELETE, spaceId);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username,
                                                                                    spaceId,
                                                                                    performedAction,
                                                                                    Messages.LOGGING_CONFIGURATION_DELETE_AUDIT_LOG_CONFIG,
                                                                                    buildIdentifiers(loggingConfiguration)),
                                                          ConfigurationChangeActions.CONFIGURATION_DELETE);
    }

    public void logGetLoggingConfiguration(String username, String spaceId, LoggingConfiguration loggingConfiguration) {
        String performedAction = MessageFormat.format(Messages.LOGGING_CONFIGURATION_GET, spaceId);
        Map<String, String> identifiers = new HashMap<>();
        identifiers.put(MTA_ID_PROPERTY_NAME, loggingConfiguration.getMtaId());
        identifiers.put(NAMESPACE_PROPERTY_NAME, loggingConfiguration.getNamespace());
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           spaceId,
                                                                           performedAction,
                                                                           Messages.LOGGING_CONFIGURATION_GET_AUDIT_LOG_CONFIG,
                                                                           identifiers));
    }

    public void logListLoggingConfigurations(String username, String spaceId) {
        String performedAction = MessageFormat.format(Messages.LOGGING_CONFIGURATION_LIST, spaceId);
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           spaceId,
                                                                           performedAction,
                                                                           Messages.LOGGING_CONFIGURATION_LIST_AUDIT_LOG_CONFIG));
    }

    private Map<String, String> buildIdentifiers(LoggingConfiguration loggingConfiguration) {
        Map<String, String> identifiers = new HashMap<>();
        identifiers.put(ID_PROPERTY_NAME, loggingConfiguration.getId());
        identifiers.put(MTA_ID_PROPERTY_NAME, loggingConfiguration.getMtaId());
        identifiers.put(MTA_SPACE_PROPERTY_NAME, loggingConfiguration.getMtaSpace());
        identifiers.put(MTA_SPACE_ID_PROPERTY_NAME, loggingConfiguration.getMtaSpaceId());
        identifiers.put(MTA_ORG_PROPERTY_NAME, loggingConfiguration.getMtaOrg());
        identifiers.put(NAMESPACE_PROPERTY_NAME, loggingConfiguration.getNamespace());
        identifiers.put(TARGET_SPACE_PROPERTY_NAME, loggingConfiguration.getTargetSpace());
        identifiers.put(TARGET_ORG_PROPERTY_NAME, loggingConfiguration.getTargetOrg());
        identifiers.put(SERVICE_INSTANCE_NAME_PROPERTY_NAME, loggingConfiguration.getServiceInstanceName());
        identifiers.put(SERVICE_KEY_NAME_PROPERTY_NAME, loggingConfiguration.getServiceKeyName());
        identifiers.put(LOG_LEVEL_PROPERTY_NAME, Objects.toString(loggingConfiguration.getLogLevel()));
        identifiers.put(IS_FAILSAFE_PROPERTY_NAME, Objects.toString(loggingConfiguration.isFailSafe()));
        return identifiers;
    }
}
