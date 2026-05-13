package org.cloudfoundry.multiapps.controller.persistence.query.providers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.query.SqlQuery;
import org.cloudfoundry.multiapps.controller.persistence.util.JdbcUtil;

public class CloudLoggingServiceConfigurationQueryProvider {

    public static final String INSERT_CLOUD_LOGGING_SERVICE_CONFIGURATION = "INSERT INTO %s (ID, TARGET_SPACE, TARGET_ORG, MTA_ID, MTA_ORG, MTA_SPACE, SERVICE_INSTANCE_NAME, SERVICE_KEY_NAME, LOG_LEVEL, IS_FAILSAFE, ADDED_AT) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    public static final String GET_CLOUD_LOGGING_CONFIGURATION = "SELECT ID, TARGET_SPACE, TARGET_ORG, MTA_ID, MTA_ORG, MTA_SPACE, SERVICE_INSTANCE_NAME, SERVICE_KEY_NAME, LOG_LEVEL, IS_FAILSAFE, ADDED_AT FROM %s WHERE MTA_ORG=? AND MTA_SPACE=? AND MTA_ID=?";
    public static final String DELETE_CLOUD_LOGGING_CONFIGURATION = "DELETE FROM %s WHERE ID=?";
    private static final String ID_COLUMN_LABEL = "id";
    private static final String TARGET_SPACE_COLUMN_LABEL = "target_space";
    private static final String TARGET_ORG_COLUMN_LABEL = "target_org";
    private static final String MTA_ID_COLUMN_LABEL = "mta_id";
    private static final String MTA_ORG_COLUMN_LABEL = "mta_org";
    private static final String MTA_SPACE_COLUMN_LABEL = "mta_space";
    private static final String SERVICE_INSTANCE_NAME_COLUMN_LABEL = "service_instance_name";
    private static final String SERVICE_KEY_NAME_COLUMN_LABEL = "service_key_name";
    private static final String LOG_LEVEL_COLUMN_LABEL = "log_level";
    private static final String IS_FAILSAFE_COLUMN_LABEL = "is_failsafe";
    private final String tableName;

    public CloudLoggingServiceConfigurationQueryProvider(String tableName) {
        this.tableName = tableName;
    }

    public SqlQuery<Integer> getStoreLoggingConfigurationQuery(LoggingConfiguration loggingConfiguration) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getStoreLoggingConfigurationQueryString());
                statement.setString(1, loggingConfiguration.getId());
                statement.setString(2, loggingConfiguration.getTargetSpace());
                statement.setString(3, loggingConfiguration.getTargetOrg());
                statement.setString(4, loggingConfiguration.getMtaId());
                statement.setString(5, loggingConfiguration.getMtaOrg());
                statement.setString(6, loggingConfiguration.getMtaSpace());
                statement.setString(7, loggingConfiguration.getServiceInstanceName());
                statement.setString(8, loggingConfiguration.getServiceKeyName());
                statement.setString(9, loggingConfiguration.getLogLevel()
                                                           .name());
                statement.setBoolean(10, loggingConfiguration.isFailSafe() == null ? true : loggingConfiguration.isFailSafe());
                statement.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));

                return statement.executeUpdate();
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<LoggingConfiguration> getGetLoggingConfigurationQuery(String mtaOrg, String mtaSpace, String mtaId) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement(getGetLoggingConfigurationQueryString());
                statement.setString(1, mtaOrg);
                statement.setString(2, mtaSpace);
                statement.setString(3, mtaId);

                resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    return getLoggingConfiguration(resultSet);
                }
                return null;
            } finally {
                JdbcUtil.closeQuietly(statement);
                JdbcUtil.closeQuietly(resultSet);
            }
        };
    }

    public SqlQuery<Integer> getDeleteLoggingConfigurationQuery(String id) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getDeleteLoggingConfigurationQueryString());
                statement.setString(1, id);
                return statement.executeUpdate();
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    private LoggingConfiguration getLoggingConfiguration(ResultSet resultSet) throws SQLException {
        return ImmutableLoggingConfiguration.builder()
                                            .id(resultSet.getString(ID_COLUMN_LABEL))
                                            .targetSpace(resultSet.getString(TARGET_SPACE_COLUMN_LABEL))
                                            .targetOrg(resultSet.getString(TARGET_ORG_COLUMN_LABEL))
                                            .mtaId(resultSet.getString(MTA_ID_COLUMN_LABEL))
                                            .mtaOrg(resultSet.getString(MTA_ORG_COLUMN_LABEL))
                                            .mtaSpace(resultSet.getString(MTA_SPACE_COLUMN_LABEL))
                                            .serviceInstanceName(resultSet.getString(SERVICE_INSTANCE_NAME_COLUMN_LABEL))
                                            .serviceKeyName(resultSet.getString(SERVICE_KEY_NAME_COLUMN_LABEL))
                                            .logLevel(LogLevel.get(resultSet.getString(LOG_LEVEL_COLUMN_LABEL)))
                                            .isFailSafe(resultSet.getBoolean(IS_FAILSAFE_COLUMN_LABEL))
                                            .build();
    }

    private String getStoreLoggingConfigurationQueryString() {
        return String.format(INSERT_CLOUD_LOGGING_SERVICE_CONFIGURATION, tableName);
    }

    private String getGetLoggingConfigurationQueryString() {
        return String.format(GET_CLOUD_LOGGING_CONFIGURATION, tableName);
    }

    private String getDeleteLoggingConfigurationQueryString() {
        return String.format(DELETE_CLOUD_LOGGING_CONFIGURATION, tableName);
    }
}
