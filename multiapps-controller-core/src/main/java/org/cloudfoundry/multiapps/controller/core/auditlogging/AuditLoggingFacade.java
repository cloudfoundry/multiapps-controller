package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;

public interface AuditLoggingFacade {

    void logSecurityIncident(AuditLogConfiguration configuration);

    void logDataAccessAuditLog(AuditLogConfiguration configuration);

    void logConfigurationChangeAuditLog(AuditLogConfiguration configuration, ConfigurationChangeActions configurationAction);
}
