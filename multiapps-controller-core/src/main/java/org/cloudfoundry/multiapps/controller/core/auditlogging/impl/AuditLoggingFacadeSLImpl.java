package org.cloudfoundry.multiapps.controller.core.auditlogging.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingFacade;
import org.cloudfoundry.multiapps.controller.core.auditlogging.UserInfoProvider;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;

import javax.sql.DataSource;

public class AuditLoggingFacadeSLImpl implements AuditLoggingFacade {

    private static final Logger LOGGER = (Logger) LogManager.getLogger(AuditLoggingFacadeSLImpl.class);
    private final AuditLogManager auditLogManager;

    public AuditLoggingFacadeSLImpl(DataSource dataSource, UserInfoProvider userInfoProvider) {
        this.auditLogManager = new AuditLogManager(dataSource, userInfoProvider);
    }

    @Override
    public void logSecurityIncident(AuditLogConfiguration configuration) {
        writeMessage(auditLogManager.getSecurityLogger(), configuration.getPerformedAction(), Level.WARN);
    }

    @Override
    public void logDataAccessAuditLog(AuditLogConfiguration configuration) {
        writeMessage(auditLogManager.getConfigLogger(), configuration.getPerformedAction(), Level.WARN);
    }

    @Override
    public void logConfigurationChangeAuditLog(AuditLogConfiguration configuration, ConfigurationChangeActions configurationAction) {
        writeMessage(auditLogManager.getConfigLogger(), configuration.getPerformedAction(), Level.WARN);
    }

    private void writeMessage(Logger logger, String message, Level level) {
        Exception loggingException = null;
        synchronized (auditLogManager) {
            logger.log(level, message);
            loggingException = auditLogManager.getException();
        }
        if (loggingException != null) {
            LOGGER.error(Messages.AUDIT_LOGGING_FAILED, loggingException);
        }
    }

}
