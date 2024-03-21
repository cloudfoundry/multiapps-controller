package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.CustomAuditLog;
import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;

public interface AuditLoggingFacade {

    void logSecurityIncident(CustomAuditLog configuration);
    void logDataAccessAuditLog(CustomAuditLog configuration);
    void logConfigurationChangeAuditLog(CustomAuditLog configuration, ConfigurationChangeActions configurationAction);
}
