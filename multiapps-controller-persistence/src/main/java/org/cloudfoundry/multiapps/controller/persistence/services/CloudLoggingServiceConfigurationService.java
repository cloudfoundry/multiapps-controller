package org.cloudfoundry.multiapps.controller.persistence.services;

import java.sql.SQLException;

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

    public LoggingConfiguration getCloudLoggingServiceConfiguration(String mtaOrg, String mtaSpace, String mtaId) {
        try {
            return getSqlQueryExecutor().execute(
                cloudLoggingServiceConfigurationQueryProvider.getGetLoggingConfigurationQuery(mtaOrg, mtaSpace, mtaId));
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

    public SqlQueryExecutor getSqlQueryExecutor() {
        return sqlQueryExecutor;
    }
}
