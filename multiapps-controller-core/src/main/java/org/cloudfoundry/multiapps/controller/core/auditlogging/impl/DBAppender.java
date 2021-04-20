package org.cloudfoundry.multiapps.controller.core.auditlogging.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.cloudfoundry.multiapps.controller.core.auditlogging.UserInfoProvider;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;

class DBAppender extends AbstractAppender {

    interface LogEventAdapter {
        void eventToStatement(String category, LogEvent event, UserInfo userInfo, PreparedStatement statement) throws SQLException;
    }

    interface ExceptionHandler {
        void handleException(Exception e);
    }

    private static final Level LEVEL = Level.INFO;
    private static final ThresholdFilter FILTER = ThresholdFilter.createFilter(LEVEL, Filter.Result.ACCEPT, Filter.Result.ACCEPT);
    private static final PatternLayout LAYOUT = PatternLayout.createDefaultLayout();
    private static final String DEFAULT_NAME = "DBAppender";

    private final DataSource dataSource;
    private final LogEventAdapter eventAdapter;
    private final String sql;
    private final ExceptionHandler exceptionHandler;
    private final UserInfoProvider userInfoProvider;
    private final String appenderName;

    DBAppender(DataSource dataSource, String sql, LogEventAdapter eventAdapter, ExceptionHandler exceptionHandler,
               UserInfoProvider userInfoProvider, String appenderName) {
        super(DEFAULT_NAME, FILTER, LAYOUT, false, null);
        this.dataSource = Objects.requireNonNull(dataSource);
        this.sql = Objects.requireNonNull(sql);
        this.eventAdapter = Objects.requireNonNull(eventAdapter);
        this.exceptionHandler = Objects.requireNonNull(exceptionHandler);
        this.userInfoProvider = userInfoProvider;
        this.appenderName = appenderName != null ? appenderName : DEFAULT_NAME;
    }

    @Override
    public void append(LogEvent event) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
            eventAdapter.eventToStatement(getName(), event, userInfoProvider.getUserInfo(), stmt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            exceptionHandler.handleException(e);
        }
    }

    @Override
    public String getName() {
        return appenderName;
    }
}
