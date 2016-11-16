package com.sap.cloud.lm.sl.cf.core.auditlogging.impl;

import java.text.MessageFormat;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingFacade;
import com.sap.cloud.lm.sl.cf.core.auditlogging.UserInfoProvider;
import com.sap.cloud.lm.sl.cf.core.message.Messages;

public class AuditLoggingFacadeSLImpl implements AuditLoggingFacade {

    private static final Logger LOGGER = Logger.getLogger(AuditLoggingFacadeSLImpl.class);
    private AuditLogManager auditLogManager;

    public AuditLoggingFacadeSLImpl(DataSource dataSource, UserInfoProvider userInfoProvider) {
        this.auditLogManager = new AuditLogManager(dataSource, userInfoProvider);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void logSecurityIncident(String message) {
        writeMessage(auditLogManager.getSecurityLogger(), message, Priority.WARN);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void logAboutToStart(String action) {
        String message = MessageFormat.format(Messages.AUDIT_LOG_ABOUT_TO_PERFORM_ACTION, action);
        writeMessage(auditLogManager.getActionLogger(), message, Priority.INFO);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void logAboutToStart(String action, Map<String, Object> parameters) {
        String message = MessageFormat.format(Messages.AUDIT_LOG_ABOUT_TO_PERFORM_ACTION_WITH_PARAMS, action, parameters);
        writeMessage(auditLogManager.getActionLogger(), message, Priority.INFO);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void logActionStarted(String action, boolean success) {
        String message = MessageFormat.format(success ? Messages.AUDIT_LOG_ACTION_SUCCESS : Messages.AUDIT_LOG_ACTION_FAILURE, action);
        writeMessage(auditLogManager.getActionLogger(), message, Priority.INFO);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void logFullConfig(String value) {
        String message = MessageFormat.format(Messages.AUDIT_LOG_FULL_CONFIG, value);
        writeMessage(auditLogManager.getConfigLogger(), message, Priority.INFO);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void logConfigUpdate(String name, Object value) {
        String message = MessageFormat.format(Messages.AUDIT_LOG_UPDATE_CONFIG, name, value);
        writeMessage(auditLogManager.getConfigLogger(), message, Priority.INFO);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void logConfigDelete(String name) {
        String message = MessageFormat.format(Messages.AUDIT_LOG_DELETE_CONFIG, name);
        writeMessage(auditLogManager.getConfigLogger(), message, Priority.INFO);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void logConfigCreate(String name, String value) {
        String message = MessageFormat.format(Messages.AUDIT_LOG_CREATE_CONFIG, name, value);
        writeMessage(auditLogManager.getConfigLogger(), message, Priority.INFO);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void logConfigUpdated(boolean success) {
        String message = success ? Messages.AUDIT_LOG_CONFIG_UPDATED : Messages.AUDIT_LOG_CONFIG_UPDATE_FAILED;
        writeMessage(auditLogManager.getConfigLogger(), message, Priority.INFO);
    }

    private void writeMessage(Logger logger, String message, Priority level) {
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
