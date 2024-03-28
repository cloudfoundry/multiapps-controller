package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.apache.logging.log4j.util.Strings;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ExtentensionAuditLog;

public class InfoApiServiceAuditLog {

    public static void auditLogGetInfo(String username) {
        AuditLoggingProvider.getFacade()
                            .logDataAccessAuditLog(new ExtentensionAuditLog(username,
                                                                            Strings.EMPTY,
                                                                            Messages.GET_INFO_FOR_API_AUDIT_LOG_CONFIG,
                                                                            Messages.API_INFO_AUDIT_LOG_CONFIG));
    }
}
