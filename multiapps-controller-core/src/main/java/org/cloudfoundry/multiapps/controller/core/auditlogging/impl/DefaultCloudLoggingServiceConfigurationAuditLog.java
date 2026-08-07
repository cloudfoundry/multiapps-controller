package org.cloudfoundry.multiapps.controller.core.auditlogging.impl;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingFacade;
import org.cloudfoundry.multiapps.controller.core.auditlogging.CloudLoggingServiceConfigurationAuditLog;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class DefaultCloudLoggingServiceConfigurationAuditLog implements CloudLoggingServiceConfigurationAuditLog {

    private final AuditLoggingFacade auditLoggingFacade;

    public DefaultCloudLoggingServiceConfigurationAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    @Override
    public void logCreateLoggingConfiguration(String username, String spaceId, LoggingConfiguration loggingConfiguration) {
        auditLoggingFacade.logConfigurationChangeAuditLog(
            createAuditLogConfiguration(Messages.LOGGING_CONFIGURATION_CREATE_AUDIT_LOG_CONFIG),
            ConfigurationChangeActions.CONFIGURATION_CREATE);
    }

    @Override
    public void logUpdateLoggingConfiguration(String username, String spaceId, LoggingConfiguration newConfiguration) {
        auditLoggingFacade.logConfigurationChangeAuditLog(
            createAuditLogConfiguration(Messages.LOGGING_CONFIGURATION_UPDATE_AUDIT_LOG_CONFIG),
            ConfigurationChangeActions.CONFIGURATION_UPDATE);
    }

    @Override
    public void logDeleteLoggingConfiguration(String username, String spaceId, LoggingConfiguration loggingConfiguration) {
        auditLoggingFacade.logConfigurationChangeAuditLog(
            createAuditLogConfiguration(Messages.LOGGING_CONFIGURATION_DELETE_AUDIT_LOG_CONFIG),
            ConfigurationChangeActions.CONFIGURATION_DELETE);
    }

    @Override
    public void logGetLoggingConfiguration(String username, String spaceId, LoggingConfiguration loggingConfiguration) {
        auditLoggingFacade.logDataAccessAuditLog(createAuditLogConfiguration(Messages.LOGGING_CONFIGURATION_GET_AUDIT_LOG_CONFIG));
    }

    private AuditLogConfiguration createAuditLogConfiguration(String message) {
        return new AuditLogConfiguration(EMPTY,
                                         EMPTY,
                                         message,
                                         Messages.LOGGING_CONFIGURATION_GET_AUDIT_LOG_CONFIG);
    }
}
