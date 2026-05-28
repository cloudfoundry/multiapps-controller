package org.cloudfoundry.multiapps.controller.persistence.query.providers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.query.SqlQuery;
import org.cloudfoundry.multiapps.controller.persistence.util.JdbcUtil;

public class CloudLoggingServiceConfigurationQueryProvider {

    public static final String INSERT_CLOUD_LOGGING_SERVICE_CONFIGURATION = "INSERT INTO %s (ID, TARGET_SPACE, TARGET_ORG, MTA_ID, MTA_ORG, MTA_SPACE, MTA_SPACE_ID, SERVICE_INSTANCE_NAME, SERVICE_KEY_NAME, LOG_LEVEL, IS_FAILSAFE, ADDED_AT, NAMESPACE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    public static final String GET_CLOUD_LOGGING_CONFIGURATION = "SELECT ID, TARGET_SPACE, TARGET_ORG, MTA_ID, MTA_ORG, MTA_SPACE, MTA_SPACE_ID, SERVICE_INSTANCE_NAME, SERVICE_KEY_NAME, LOG_LEVEL, IS_FAILSAFE, NAMESPACE FROM %s WHERE MTA_SPACE=? AND MTA_ID=? AND NAMESPACE=?";
    public static final String GET_CLOUD_LOGGING_CONFIGURATION_NULL_NAMESPACE = "SELECT ID, TARGET_SPACE, TARGET_ORG, MTA_ID, MTA_ORG, MTA_SPACE, MTA_SPACE_ID, SERVICE_INSTANCE_NAME, SERVICE_KEY_NAME, LOG_LEVEL, IS_FAILSAFE, NAMESPACE FROM %s WHERE MTA_SPACE=? AND MTA_ID=? AND NAMESPACE IS NULL";
    public static final String GET_ALL_CLOUD_LOGGING_CONFIGURATIONS = "SELECT ID, TARGET_SPACE, TARGET_ORG, MTA_ID, MTA_ORG, MTA_SPACE, MTA_SPACE_ID, SERVICE_INSTANCE_NAME, SERVICE_KEY_NAME, LOG_LEVEL, IS_FAILSAFE FROM %s WHERE MTA_SPACE_ID=?";
    public static final String DELETE_CLOUD_LOGGING_CONFIGURATION = "DELETE FROM %s WHERE ID=?";
    public static final String UPDATE_CLOUD_LOGGING_CONFIGURATION = "UPDATE %s SET TARGET_SPACE=?, TARGET_ORG=?, SERVICE_INSTANCE_NAME=?, SERVICE_KEY_NAME=?, LOG_LEVEL=?, IS_FAILSAFE=?, ADDED_AT=? WHERE MTA_SPACE=? AND MTA_ID=? AND NAMESPACE=?";
    public static final String UPDATE_CLOUD_LOGGING_CONFIGURATION_NULL_NAMESPACE = "UPDATE %s SET TARGET_SPACE=?, TARGET_ORG=?, SERVICE_INSTANCE_NAME=?, SERVICE_KEY_NAME=?, LOG_LEVEL=?, IS_FAILSAFE=?, ADDED_AT=? WHERE MTA_SPACE=? AND MTA_ID=? AND NAMESPACE IS NULL";
    private static final String ID_COLUMN_LABEL = "id";
    private static final String TARGET_SPACE_COLUMN_LABEL = "target_space";
    private static final String TARGET_ORG_COLUMN_LABEL = "target_org";
    private static final String MTA_ID_COLUMN_LABEL = "mta_id";
    private static final String MTA_ORG_COLUMN_LABEL = "mta_org";
    private static final String MTA_SPACE_COLUMN_LABEL = "mta_space";
    private static final String MTA_SPACE_ID_COLUMN_LABEL = "mta_space_id";
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
                statement.setString(7, loggingConfiguration.getMtaSpaceId());
                statement.setString(8, loggingConfiguration.getServiceInstanceName());
                statement.setString(9, loggingConfiguration.getServiceKeyName());
                statement.setString(10, loggingConfiguration.getLogLevel()
                                                            .name());
                statement.setBoolean(11, loggingConfiguration.isFailSafe() == null ? true : loggingConfiguration.isFailSafe());
                statement.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
                statement.setString(13, loggingConfiguration.getNamespace());

                return statement.executeUpdate();
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<LoggingConfiguration> getGetLoggingConfigurationQuery(String mtaSpace, String mtaId, String namespace) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                if (namespace == null) {
                    statement = connection.prepareStatement(String.format(GET_CLOUD_LOGGING_CONFIGURATION_NULL_NAMESPACE, tableName));
                } else {
                    statement = connection.prepareStatement(String.format(GET_CLOUD_LOGGING_CONFIGURATION, tableName));
                    statement.setString(3, namespace);
                }
                statement.setString(1, mtaSpace);
                statement.setString(2, mtaId);
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

    public SqlQuery<Integer> getUpdateLoggingConfigurationQuery(LoggingConfiguration loggingConfiguration) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                String queryTemplate = loggingConfiguration.getNamespace() == null
                    ? UPDATE_CLOUD_LOGGING_CONFIGURATION_NULL_NAMESPACE
                    : UPDATE_CLOUD_LOGGING_CONFIGURATION;
                statement = connection.prepareStatement(String.format(queryTemplate, tableName));
                statement.setString(1, loggingConfiguration.getTargetSpace());
                statement.setString(2, loggingConfiguration.getTargetOrg());
                statement.setString(3, loggingConfiguration.getServiceInstanceName());
                statement.setString(4, loggingConfiguration.getServiceKeyName());
                statement.setString(5, loggingConfiguration.getLogLevel()
                                                           .name());
                statement.setBoolean(6, loggingConfiguration.isFailSafe() == null ? true : loggingConfiguration.isFailSafe());
                statement.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                statement.setString(8, loggingConfiguration.getMtaSpace());
                statement.setString(9, loggingConfiguration.getMtaId());
                if (loggingConfiguration.getNamespace() != null) {
                    statement.setString(10, loggingConfiguration.getNamespace());
                }
                return statement.executeUpdate();
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<List<LoggingConfiguration>> getAllCloudLoggingServiceConfigurationsFromSpace(String spaceId) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement(String.format(GET_ALL_CLOUD_LOGGING_CONFIGURATIONS, tableName));
                statement.setString(1, spaceId);
                resultSet = statement.executeQuery();
                List<LoggingConfiguration> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(getLoggingConfiguration(resultSet));
                }
                return result;
            } finally {
                JdbcUtil.closeQuietly(statement);
                JdbcUtil.closeQuietly(resultSet);
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
                                            .mtaSpaceId(resultSet.getString(MTA_SPACE_ID_COLUMN_LABEL))
                                            .serviceInstanceName(resultSet.getString(SERVICE_INSTANCE_NAME_COLUMN_LABEL))
                                            .serviceKeyName(resultSet.getString(SERVICE_KEY_NAME_COLUMN_LABEL))
                                            .logLevel(LogLevel.get(resultSet.getString(LOG_LEVEL_COLUMN_LABEL)))
                                            .isFailSafe(resultSet.getBoolean(IS_FAILSAFE_COLUMN_LABEL))
                                            .build();
    }

    private String getStoreLoggingConfigurationQueryString() {
        return String.format(INSERT_CLOUD_LOGGING_SERVICE_CONFIGURATION, tableName);
    }

    private String getDeleteLoggingConfigurationQueryString() {
        return String.format(DELETE_CLOUD_LOGGING_CONFIGURATION, tableName);
    }
}
