package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;

public interface AuditLoggingFacade {

    void logSecurityIncident(AuditLogConfiguration configuration);

    void logDataAccessAuditLog(AuditLogConfiguration configuration);

    void logConfigurationChangeAuditLog(AuditLogConfiguration configuration, ConfigurationChangeActions configurationAction);

    void logConfigurationChangeAuditLog(AuditLogConfiguration configuration,
                                        ConfigurationChangeActions configurationAction,
                                        AuditableConfiguration oldValue,
                                        AuditableConfiguration newValue);

}
