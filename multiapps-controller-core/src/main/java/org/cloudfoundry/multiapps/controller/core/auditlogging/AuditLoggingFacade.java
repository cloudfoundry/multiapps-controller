package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.util.Map;

import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;

public interface AuditLoggingFacade {

    void logSecurityIncident(String message);

    void logAboutToStart(String action);

    void logAboutToStart(String action, Map<String, Object> parameters);

    void logActionStarted(String action, boolean success);

    void logConfig(AuditableConfiguration configuration);

    void logConfigCreate(AuditableConfiguration configuration);

    void logConfigUpdate(AuditableConfiguration configuration);

    void logConfigDelete(AuditableConfiguration configuration);

    void logConfigUpdated(boolean success);

}
