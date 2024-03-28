package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ExtentensionAuditLog;

public class MtasApiServiceAuditLog {

    public static final String MTA_NAME_PROPERTY_NAME = "mtaName";
    public static final String NAMESPACE_PROPERTY_NAME = "namespace";
    public static final String NAME_PROPERTY_NAME = "name";

    public static void auditLogGetMtas(String username, String spaceId) {
        String performedAction = MessageFormat.format(Messages.LIST_MTA_AUDIT_LOG_MESSAGE, spaceId);
        AuditLoggingProvider.getFacade()
                            .logDataAccessAuditLog(new ExtentensionAuditLog(username,
                                                                            spaceId,
                                                                            performedAction,
                                                                            Messages.MTA_LIST_AUDIT_LOG_CONFIG));
    }

    public static void auditLogGetMta(String username, String spaceId, String mtaId) {
        String performedAction = MessageFormat.format(Messages.GET_MTA_AUDIT_LOG_MESSAGE, spaceId);
        Map<String, String> configIdentifiers = createAuditLogGetMtaConfigurationIdentifier(mtaId);
        AuditLoggingProvider.getFacade()
                            .logDataAccessAuditLog(new ExtentensionAuditLog(username,
                                                                            spaceId,
                                                                            performedAction,
                                                                            Messages.MTA_INFO_AUDIT_LOG_CONFIG,
                                                                            configIdentifiers));
    }

    public static void auditLogGetMtas(String username, String spaceId, String namespace, String name) {
        String performedAction = MessageFormat.format(Messages.LIST_MTA_AUDIT_LOG_MESSAGE, spaceId);
        Map<String, String> configIdentifiers = createAuditLogGetMtasConfigurationIdentifier(namespace, name);
        AuditLoggingProvider.getFacade()
                            .logDataAccessAuditLog(new ExtentensionAuditLog(username,
                                                                            spaceId,
                                                                            performedAction,
                                                                            Messages.MTA_LIST_AUDIT_LOG_CONFIG,
                                                                            configIdentifiers));
    }

    private static Map<String, String> createAuditLogGetMtasConfigurationIdentifier(String namespace, String name) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(NAME_PROPERTY_NAME, name);
        identifiers.put(NAMESPACE_PROPERTY_NAME, namespace);

        return identifiers;
    }

    private static Map<String, String> createAuditLogGetMtaConfigurationIdentifier(String mtaName) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(MTA_NAME_PROPERTY_NAME, mtaName);

        return identifiers;
    }
}
