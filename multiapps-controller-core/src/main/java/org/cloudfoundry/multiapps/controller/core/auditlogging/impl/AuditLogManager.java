package org.cloudfoundry.multiapps.controller.core.auditlogging.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.cloudfoundry.multiapps.controller.core.auditlogging.UserInfoProvider;
import org.cloudfoundry.multiapps.controller.core.auditlogging.impl.DBAppender.LogEventAdapter;

import java.sql.Timestamp;
import javax.sql.DataSource;

class AuditLogManager {

    private static final String AUDIT_LOG_INSERT_STATEMENT = "INSERT INTO AUDIT_LOG (USER, MODIFIED, CATEGORY, SEVERITY, MESSAGE) VALUES (?, ?, ?, ?, ?)";

    private static final LogEventAdapter EVENT_ADAPTER = (category, event, userInfo, stmt) -> {
        stmt.setString(1, userInfo == null ? null : userInfo.getName());
        stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        stmt.setString(3, category);
        stmt.setString(4, event.getLevel()
                               .toString());
        stmt.setString(5, event.getMessage()
                               .toString());
    };

    private final AuditLoggingExceptionHandler exceptionHandler = new AuditLoggingExceptionHandler();

    private Logger securityLogger = null;

    private final Logger configLogger;

    private final Logger actionLogger;

    Logger getSecurityLogger() {
        return securityLogger;
    }

    Logger getConfigLogger() {
        return configLogger;
    }

    Logger getActionLogger() {
        return actionLogger;
    }

    Exception getException() {
        return exceptionHandler.getException();
    }

    AuditLogManager(DataSource dataSource, UserInfoProvider userInfoProvider) {
        securityLogger = setUpLogger(dataSource, userInfoProvider, "SECURITY");
        configLogger = setUpLogger(dataSource, userInfoProvider, "CONFIG");
        actionLogger = setUpLogger(dataSource, userInfoProvider, "ACTION");
    }

    private Logger setUpLogger(DataSource dataSource, UserInfoProvider userInfoProvider, String name) {
        try (LoggerContext loggerContext = new LoggerContext(name)) {
            DBAppender auditLogAppender = initializeDBAppender(dataSource, AUDIT_LOG_INSERT_STATEMENT, EVENT_ADAPTER, exceptionHandler,
                                                               userInfoProvider, name);
            auditLogAppender.start();
            loggerContext.getConfiguration()
                         .addAppender(auditLogAppender);
            LoggerConfig loggerConfig = initializeLoggerConfig(loggerContext);
            addAppenderToRootLogger(loggerContext, auditLogAppender);
            return (Logger) LogManager.getLogger(loggerConfig);
        }
    }

    private DBAppender initializeDBAppender(DataSource dataSource, String logInsertStatement, LogEventAdapter logEventAdapter,
                                            AuditLoggingExceptionHandler exceptionHandler, UserInfoProvider userInfoProvider, String name) {
        return new DBAppender(dataSource, logInsertStatement, logEventAdapter, exceptionHandler, userInfoProvider, name);
    }

    private LoggerConfig initializeLoggerConfig(LoggerContext loggerContext) {
        LoggerConfig loggerConfig = loggerContext.getConfiguration()
                                                 .getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(Level.INFO);
        return loggerConfig;
    }

    private void addAppenderToRootLogger(LoggerContext loggerContext, Appender auditLogAppender) {
        loggerContext.getRootLogger()
                     .addAppender(loggerContext.getConfiguration()
                                               .getAppender(auditLogAppender.getName()));
        loggerContext.updateLoggers();
    }
}
