package com.sap.cloud.lm.sl.cf.core.auditlogging.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;

import com.sap.cloud.lm.sl.cf.core.auditlogging.UserInfoProvider;
import com.sap.cloud.lm.sl.cf.core.auditlogging.impl.DBAppender.LogEventAdapter;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;

class AuditLogManager {

    private static final String AUDIT_LOG_INSERT_STATEMENT = "INSERT INTO AUDIT_LOG (USER, MODIFIED, CATEGORY, SEVERITY, MESSAGE) VALUES (?, ?, ?, ?, ?)";

    private static final LogEventAdapter EVENT_ADAPTER = new LogEventAdapter() {
        @Override
        public void eventToStatement(String category, LogEvent event, UserInfo userInfo, PreparedStatement stmt) throws SQLException {
            stmt.setString(1, userInfo == null ? null : userInfo.getName());
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            stmt.setString(3, category);
            stmt.setString(4, event.getLevel()
                                   .toString());
            stmt.setString(5, event.getMessage()
                                   .toString());
        }
    };

    private final AuditLoggingExceptionHandler exceptionHandler = new AuditLoggingExceptionHandler();

    private final Logger securityLogger;
    private final Logger configLogger;
    private final Logger actionLogger;

    AuditLogManager(DataSource dataSource, UserInfoProvider userInfoProvider) {
        securityLogger = setUpLogger(dataSource, userInfoProvider, "SECURITY");
        configLogger = setUpLogger(dataSource, userInfoProvider, "CONFIG");
        actionLogger = setUpLogger(dataSource, userInfoProvider, "ACTION");
    }

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

    private Logger setUpLogger(DataSource dataSource, UserInfoProvider userInfoProvider, String name) {
        try (LoggerContext loggerContext = new LoggerContext(name)) {
            DBAppender auditLogAppender = new DBAppender(dataSource,
                                                         AUDIT_LOG_INSERT_STATEMENT,
                                                         EVENT_ADAPTER,
                                                         exceptionHandler,
                                                         userInfoProvider,
                                                         name);
            auditLogAppender.start();

            loggerContext.getConfiguration()
                         .addAppender(auditLogAppender);
            LoggerConfig loggerConfig = loggerContext.getConfiguration()
                                                     .getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
            loggerConfig.setLevel(Level.INFO);
            loggerContext.getRootLogger()
                         .addAppender(loggerContext.getConfiguration()
                                                   .getAppender(auditLogAppender.getName()));
            loggerContext.updateLoggers();
            return (Logger) LogManager.getLogger(loggerConfig);
        }
    }

}
