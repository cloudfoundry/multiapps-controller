package com.sap.cloud.lm.sl.cf.core.auditlogging;

import java.util.Map;

public interface AuditLoggingFacade {

    void logSecurityIncident(String message);

    void logAboutToStart(String action);

    void logAboutToStart(String action, Map<String, Object> parameters);

    void logActionStarted(String action, boolean success);

    void logConfig(String name, Object value);

    void logConfigCreate(String name);

    void logConfigUpdate(String name);

    void logConfigDelete(String name);

    void logConfigUpdated(boolean success);

}
