package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ExtentensionAuditLog;

public class OperationsApiServiceAuditLog {

    public static final String MTA_ID_PROPERTY_NAME = "mtaId";
    public static final String LOG_ID_PROPERTY_NAME = "logId";
    public static final String ACTION_ID_PROPERTY_NAME = "actionId";
    public static final String OPERATION_ID_PROPERTY_NAME = "operationId";
    public static final String PROCESS_TYPE_PROPERTY_NAME = "processType";
    public static final String PROCESS_ID_PROPERTY_NAME = "processId";
    public static final String EMBED_PROPERTY_NAME = "embed";

    public static void auditLogGetOperations(String username, String spaceId, String mtaId) {
        String performedAction = MessageFormat.format(Messages.LIST_OPERATIONS_AUDIT_LOG_MESSAGE, spaceId);
        Map<String, String> configIdentifiers = createAuditLogGetOperationsConfigurationIdentifier(mtaId);
        AuditLoggingProvider.getFacade()
                            .logDataAccessAuditLog(new ExtentensionAuditLog(username,
                                                                            spaceId,
                                                                            performedAction,
                                                                            Messages.OPERATION_LIST_AUDIT_LOG_CONFIG,
                                                                            configIdentifiers));
    }

    public static void auditLogExecuteOperationAction(String username, String spaceId, String operationId, String actionId) {
        String performedAction = MessageFormat.format(Messages.EXECUTE_OPERATION_AUDIT_LOG_MESSAGE, spaceId);
        Map<String, String> configIdentifiers = createAuditLogExecuteOperationActionConfigurationIdentifier(operationId, actionId);
        AuditLoggingProvider.getFacade()
                            .logConfigurationChangeAuditLog(new ExtentensionAuditLog(username,
                                                                                     spaceId,
                                                                                     performedAction,
                                                                                     Messages.EXECUTE_OPERATION_AUDIT_LOG_CONFIG,
                                                                                     configIdentifiers),
                                                            ConfigurationChangeActions.CONFIGURATION_CREATE);
    }

    public static void auditLogGetOperationLogs(String username, String spaceId, String operationId) {
        String performedAction = MessageFormat.format(Messages.GET_OPERATION_LOGS_AUDIT_LOG_MESSAGE, spaceId);
        Map<String, String> configIdentifiers = createAuditLogGetOperationLogsConfigurationIdentifier(operationId);
        AuditLoggingProvider.getFacade()
                            .logDataAccessAuditLog(new ExtentensionAuditLog(username,
                                                                            spaceId,
                                                                            performedAction,
                                                                            Messages.LIST_OPERATION_LOGS_AUDIT_LOG_CONFIG,
                                                                            configIdentifiers));
    }

    public static void auditLogGetOperationLogContent(String username, String spaceId, String operationId, String logId) {
        String performedAction = MessageFormat.format(Messages.GET_OPERATION_LOG_CONTENT_AUDIT_LOG_MESSAGE, spaceId);
        Map<String, String> configIdentifiers = createAuditLogGetOperationLogContentConfigurationIdentifier(operationId, logId);
        AuditLoggingProvider.getFacade()
                            .logDataAccessAuditLog(new ExtentensionAuditLog(username,
                                                                            spaceId,
                                                                            performedAction,
                                                                            Messages.GET_OPERATION_LOG_CONTENT_AUDIT_LOG_CONFIG,
                                                                            configIdentifiers));
    }

    public static void auditLogStartOperation(String username, String spaceId, Operation operation) {
        String performedAction = MessageFormat.format(Messages.START_OPERATION_AUDIT_LOG_MESSAGE, operation.getProcessType()
                                                                                                           .getName(),
                                                      spaceId);
        Map<String, String> configIdentifiers = createAuditLogStartOperationConfigurationIdentifier(operation);
        AuditLoggingProvider.getFacade()
                            .logConfigurationChangeAuditLog(new ExtentensionAuditLog(username,
                                                                                     spaceId,
                                                                                     performedAction,
                                                                                     Messages.START_OPERATION_AUDIT_LOG_CONFIG,
                                                                                     configIdentifiers),
                                                            ConfigurationChangeActions.CONFIGURATION_CREATE);
    }

    public static void auditLogGetOperation(String username, String spaceId, String operationId, String embed) {
        String performedAction = MessageFormat.format(Messages.GET_INFO_FOR_OPERATION, spaceId);
        Map<String, String> configIdentifiers = createAuditLogGetOperationConfigurationIdentifier(operationId, embed);
        AuditLoggingProvider.getFacade()
                            .logDataAccessAuditLog(new ExtentensionAuditLog(username,
                                                                            spaceId,
                                                                            performedAction,
                                                                            Messages.GET_OPERATION_INFO_AUDIT_LOG_CONFIG,
                                                                            configIdentifiers));
    }

    private static Map<String, String> createAuditLogGetOperationsConfigurationIdentifier(String mtaId) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(MTA_ID_PROPERTY_NAME, mtaId);

        return identifiers;
    }

    private static Map<String, String> createAuditLogGetOperationConfigurationIdentifier(String operationId, String embed) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(OPERATION_ID_PROPERTY_NAME, operationId);
        identifiers.put(EMBED_PROPERTY_NAME, embed);

        return identifiers;
    }

    private static Map<String, String> createAuditLogExecuteOperationActionConfigurationIdentifier(String operationId, String actionId) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(ACTION_ID_PROPERTY_NAME, actionId);
        identifiers.put(OPERATION_ID_PROPERTY_NAME, operationId);

        return identifiers;
    }

    private static Map<String, String> createAuditLogGetOperationLogsConfigurationIdentifier(String operationId) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(OPERATION_ID_PROPERTY_NAME, operationId);

        return identifiers;
    }

    private static Map<String, String> createAuditLogGetOperationLogContentConfigurationIdentifier(String operationId, String logId) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(OPERATION_ID_PROPERTY_NAME, operationId);
        identifiers.put(LOG_ID_PROPERTY_NAME, logId);

        return identifiers;
    }

    private static Map<String, String> createAuditLogStartOperationConfigurationIdentifier(Operation operation) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(PROCESS_TYPE_PROPERTY_NAME, operation.getProcessType()
                                                             .getName());
        identifiers.put(PROCESS_ID_PROPERTY_NAME, operation.getProcessId());
        identifiers.put(MTA_ID_PROPERTY_NAME, operation.getMtaId());

        return identifiers;
    }
}
