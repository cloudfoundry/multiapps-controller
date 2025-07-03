package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;

public class ApplicationConfigurationAuditLog {

    private final AuditLoggingFacade auditLoggingFacade;

    public ApplicationConfigurationAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void logEnvironmentVariableRead(String envVariableName, String spaceGuid) {
        String performedAction = MessageFormat.format(Messages.READ_ENV_FROM_ENVIRONMENT, envVariableName, spaceGuid);
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(spaceGuid,
                                                                           performedAction,
                                                                           Messages.ENVIRONMENT_VARIABLE_AUDIT_LOG_CONFIG));
    }
}
