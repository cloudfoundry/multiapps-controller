package com.sap.cloud.lm.sl.cf.core.auditlogging.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import javax.sql.DataSource;

import org.apache.log4j.spi.LoggingEvent;

import com.sap.cloud.lm.sl.cf.core.auditlogging.UserInfoProvider;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;

class DBAppender extends org.apache.log4j.AppenderSkeleton implements org.apache.log4j.Appender {

    interface LogEventAdapter {
        void eventToStatement(String category, LoggingEvent event, UserInfo userInfo, PreparedStatement statement) throws SQLException;
    }

    interface ExceptionHandler {
        void handleException(Exception e);
    }

    private final DataSource dataSource;
    private final LogEventAdapter eventAdapter;
    private final String sql;
    private final ExceptionHandler exceptionHandler;
    private final UserInfoProvider userInfoProvider;

    DBAppender(DataSource dataSource, String sql, LogEventAdapter eventAdapter, ExceptionHandler exceptionHandler,
               UserInfoProvider userInfoProvider) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.sql = Objects.requireNonNull(sql);
        this.eventAdapter = Objects.requireNonNull(eventAdapter);
        this.exceptionHandler = Objects.requireNonNull(exceptionHandler);
        this.userInfoProvider = userInfoProvider;
    }

    @Override
    protected void append(LoggingEvent event) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
            eventAdapter.eventToStatement(getName(), event, userInfoProvider.getUserInfo(), stmt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            exceptionHandler.handleException(e);
        }
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    @Override
    public void close() {
        // Nothing to free here, connections are released to pool after being used
    }

}
