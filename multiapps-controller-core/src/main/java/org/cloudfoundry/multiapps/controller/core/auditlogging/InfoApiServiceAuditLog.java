package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.apache.logging.log4j.util.Strings;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;

public class InfoApiServiceAuditLog {

    private final AuditLoggingFacade auditLoggingFacade;

    public InfoApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void logGetInfo(String username) {
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           Strings.EMPTY,
                                                                           Messages.GET_INFO_FOR_API_AUDIT_LOG_CONFIG,
                                                                           Messages.API_INFO_AUDIT_LOG_CONFIG));
    }
}
