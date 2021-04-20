package org.cloudfoundry.multiapps.controller.core.auditlogging.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingFacade;
import org.cloudfoundry.multiapps.controller.core.auditlogging.UserInfoProvider;
import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;

import java.text.MessageFormat;
import java.util.Map;
import javax.sql.DataSource;

public class AuditLoggingFacadeSLImpl implements AuditLoggingFacade {

    private static final Logger LOGGER = (Logger) LogManager.getLogger(AuditLoggingFacadeSLImpl.class);
    private final AuditLogManager auditLogManager;

    public AuditLoggingFacadeSLImpl(DataSource dataSource, UserInfoProvider userInfoProvider) {
        this.auditLogManager = new AuditLogManager(dataSource, userInfoProvider);
    }

    @Override
    public void logSecurityIncident(String message) {
        writeMessage(auditLogManager.getSecurityLogger(), message, Level.WARN);
    }

    @Override
    public void logAboutToStart(String action) {
        String message = MessageFormat.format(Messages.AUDIT_LOG_ABOUT_TO_PERFORM_ACTION, action);
        writeMessage(auditLogManager.getActionLogger(), message, Level.INFO);
    }

    @Override
    public void logAboutToStart(String action, Map<String, Object> parameters) {
        String message = MessageFormat.format(Messages.AUDIT_LOG_ABOUT_TO_PERFORM_ACTION_WITH_PARAMS, action, parameters);
        writeMessage(auditLogManager.getActionLogger(), message, Level.INFO);
    }

    @Override
    public void logActionStarted(String action, boolean success) {
        String message = MessageFormat.format(success ? Messages.AUDIT_LOG_ACTION_SUCCESS : Messages.AUDIT_LOG_ACTION_FAILURE, action);
        writeMessage(auditLogManager.getActionLogger(), message, Level.INFO);
    }

    @Override
    public void logConfig(AuditableConfiguration configuration) {
        String message = MessageFormat.format(Messages.AUDIT_LOG_CONFIG, configuration.getConfigurationType(),
                                              configuration.getConfigurationName());
        writeMessage(auditLogManager.getConfigLogger(), message, Level.INFO);
    }

    @Override
    public void logConfigUpdate(AuditableConfiguration configuration) {
        String message = MessageFormat.format(Messages.AUDIT_LOG_UPDATE_CONFIG, configuration.getConfigurationName());
        writeMessage(auditLogManager.getConfigLogger(), message, Level.INFO);
    }

    @Override
    public void logConfigDelete(AuditableConfiguration configuration) {
        String message = MessageFormat.format(Messages.AUDIT_LOG_DELETE_CONFIG, configuration.getConfigurationName());
        writeMessage(auditLogManager.getConfigLogger(), message, Level.INFO);
    }

    @Override
    public void logConfigCreate(AuditableConfiguration configuration) {
        String message = MessageFormat.format(Messages.AUDIT_LOG_CREATE_CONFIG, configuration.getConfigurationType(),
                                              configuration.getConfigurationName());
        writeMessage(auditLogManager.getConfigLogger(), message, Level.INFO);
    }

    @Override
    public void logConfigUpdated(boolean success) {
        String message = success ? Messages.AUDIT_LOG_CONFIG_UPDATED : Messages.AUDIT_LOG_CONFIG_UPDATE_FAILED;
        writeMessage(auditLogManager.getConfigLogger(), message, Level.INFO);
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
