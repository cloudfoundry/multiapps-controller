package com.sap.cloud.lm.sl.cf.core.auditlogging;

import java.util.Map;

public interface AuditLoggingFacade {

    void logSecurityIncident(String message);

    void logAboutToStart(String action);

    void logAboutToStart(String action, Map<String, Object> parameters);

    void logActionStarted(String action, boolean success);

    void logFullConfig(String value);

    void logConfigUpdate(String name, Object value);

    void logConfigDelete(String name);

    void logConfigUpdated(boolean success);

    void logConfigCreate(String name, String xml);

}
