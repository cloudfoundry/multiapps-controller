package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;

public interface CloudLoggingServiceConfigurationAuditLog {

    void logCreateLoggingConfiguration(String username, String spaceId, LoggingConfiguration loggingConfiguration);

    void logUpdateLoggingConfiguration(String username, String spaceId, LoggingConfiguration newConfiguration);

    void logDeleteLoggingConfiguration(String username, String spaceId, LoggingConfiguration loggingConfiguration);

    void logGetLoggingConfiguration(String username, String spaceId, LoggingConfiguration loggingConfiguration);
}
