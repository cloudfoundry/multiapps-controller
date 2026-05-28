package org.cloudfoundry.multiapps.controller.persistence.services;

import java.sql.SQLException;
import java.util.List;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.query.providers.CloudLoggingServiceConfigurationQueryProvider;
import org.cloudfoundry.multiapps.controller.persistence.util.SqlQueryExecutor;

@Named("cloudLoggingServiceConfigurationService")
public class CloudLoggingServiceConfigurationService {

    public static final String TABLE_NAME = "cloud_logging_service_configuration";
    private final SqlQueryExecutor sqlQueryExecutor;

    private final CloudLoggingServiceConfigurationQueryProvider cloudLoggingServiceConfigurationQueryProvider;

    public CloudLoggingServiceConfigurationService(DataSourceWithDialect dataSourceWithDialect) {
        cloudLoggingServiceConfigurationQueryProvider = new CloudLoggingServiceConfigurationQueryProvider(TABLE_NAME);
        this.sqlQueryExecutor = new SqlQueryExecutor(dataSourceWithDialect.getDataSource());
    }

    public void storeCloudLoggingServiceConfiguration(LoggingConfiguration loggingConfiguration) {
        try {
            getSqlQueryExecutor().execute(
                cloudLoggingServiceConfigurationQueryProvider.getStoreLoggingConfigurationQuery(loggingConfiguration));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public LoggingConfiguration getCloudLoggingServiceConfiguration(String mtaSpace, String mtaId, String namespace) {
        try {
            return getSqlQueryExecutor().execute(
                cloudLoggingServiceConfigurationQueryProvider.getGetLoggingConfigurationQuery(mtaSpace, mtaId, namespace));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteCloudLoggingServiceConfiguration(String id) {
        try {
            getSqlQueryExecutor().execute(cloudLoggingServiceConfigurationQueryProvider.getDeleteLoggingConfigurationQuery(id));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateCloudLoggingServiceConfiguration(LoggingConfiguration loggingConfiguration) {
        try {
            getSqlQueryExecutor().execute(
                cloudLoggingServiceConfigurationQueryProvider.getUpdateLoggingConfigurationQuery(loggingConfiguration));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<LoggingConfiguration> getAllCloudLoggingServiceConfigurationsFromSpace(String spaceId) {
        try {
            return getSqlQueryExecutor().execute(
                cloudLoggingServiceConfigurationQueryProvider.getAllCloudLoggingServiceConfigurationsFromSpace(spaceId));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public SqlQueryExecutor getSqlQueryExecutor() {
        return sqlQueryExecutor;
    }
}
