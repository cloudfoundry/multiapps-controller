package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingFacade;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;

public class FlowableSlmpResourceAuditLog {

    private final AuditLoggingFacade auditLoggingFacade;

    public FlowableSlmpResourceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void auditLogConfigurationChange(String username, String spaceId, String action, String configuration,
                                            ConfigurationChangeActions configurationAction) {
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username, spaceId, action, configuration),
                                                          configurationAction);
    }

    public void auditLogActionPerformed(String username, String spaceId, String action, String configuration) {
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username, spaceId, action, configuration));
    }

    public void auditLogActionPerformed(String username, String spaceId, String action, String configuration,
                                        Map<String, String> parameters) {
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username, spaceId, action, configuration, parameters));
    }
}
