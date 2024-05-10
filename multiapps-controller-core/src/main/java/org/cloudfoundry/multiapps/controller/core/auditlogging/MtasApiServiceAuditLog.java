package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;

public class MtasApiServiceAuditLog {

    public static final String MTA_NAME_PROPERTY_NAME = "mtaName";
    public static final String NAMESPACE_PROPERTY_NAME = "namespace";
    public static final String NAME_PROPERTY_NAME = "name";

    private final AuditLoggingFacade auditLoggingFacade;

    public MtasApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void logGetMtas(String username, String spaceId) {
        String performedAction = MessageFormat.format(Messages.LIST_MTA_AUDIT_LOG_MESSAGE, spaceId);
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           spaceId,
                                                                           performedAction,
                                                                           Messages.MTA_LIST_AUDIT_LOG_CONFIG));
    }

    public void logGetMta(String username, String spaceId, String mtaId) {
        String performedAction = MessageFormat.format(Messages.GET_MTA_AUDIT_LOG_MESSAGE, spaceId);
        Map<String, String> configIdentifiers = createAuditLogGetMtaConfigurationIdentifier(mtaId);
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           spaceId,
                                                                           performedAction,
                                                                           Messages.MTA_INFO_AUDIT_LOG_CONFIG,
                                                                           configIdentifiers));
    }

    public void logGetMtas(String username, String spaceId, String namespace, String name) {
        String performedAction = MessageFormat.format(Messages.LIST_MTA_AUDIT_LOG_MESSAGE, spaceId);
        Map<String, String> configIdentifiers = createAuditLogGetMtasConfigurationIdentifier(namespace, name);
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           spaceId,
                                                                           performedAction,
                                                                           Messages.MTA_LIST_AUDIT_LOG_CONFIG,
                                                                           configIdentifiers));
    }

    private Map<String, String> createAuditLogGetMtasConfigurationIdentifier(String namespace, String name) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(NAME_PROPERTY_NAME, name);
        identifiers.put(NAMESPACE_PROPERTY_NAME, namespace);

        return identifiers;
    }

    private Map<String, String> createAuditLogGetMtaConfigurationIdentifier(String mtaName) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(MTA_NAME_PROPERTY_NAME, mtaName);

        return identifiers;
    }
}
