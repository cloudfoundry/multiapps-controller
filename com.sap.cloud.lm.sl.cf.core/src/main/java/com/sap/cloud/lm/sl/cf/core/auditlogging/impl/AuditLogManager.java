package com.sap.cloud.lm.sl.cf.core.auditlogging.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import com.sap.cloud.lm.sl.cf.core.auditlogging.UserInfoProvider;
import com.sap.cloud.lm.sl.cf.core.auditlogging.impl.DBAppender.LogEventAdapter;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;

class AuditLogManager {

    private static final String AUDIT_LOG_INSERT_STATEMENT = "INSERT INTO AUDIT_LOG (USER, MODIFIED, CATEGORY, SEVERITY, MESSAGE) VALUES (?, ?, ?, ?, ?)";

    private static final LogEventAdapter EVENT_ADAPTER = new LogEventAdapter() {
        @Override
        public void eventToStatement(String category, LoggingEvent event, UserInfo userInfo, PreparedStatement stmt) throws SQLException {
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

    private Logger securityLogger = null;

    private Logger configLogger;

    private Logger actionLogger;

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
        Logger logger = Logger.getLogger(name);
        DBAppender auditLogAppender = new DBAppender(dataSource,
                                                     AUDIT_LOG_INSERT_STATEMENT,
                                                     EVENT_ADAPTER,
                                                     exceptionHandler,
                                                     userInfoProvider);
        auditLogAppender.setName(name);
        logger.addAppender(auditLogAppender);
        return logger;
    }

}
