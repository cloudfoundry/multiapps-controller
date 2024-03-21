package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;

public class OperationsApiServiceAuditLog {

    private static final String MTA_ID_PROPERTY_NAME = "mtaId";
    private static final String LOG_ID_PROPERTY_NAME = "logId";
    private static final String ACTION_ID_PROPERTY_NAME = "actionId";
    private static final String OPERATION_ID_PROPERTY_NAME = "operationId";
    private static final String PROCESS_TYPE_PROPERTY_NAME = "processType";
    private static final String PROCESS_ID_PROPERTY_NAME = "processId";
    private static final String EMBED_PROPERTY_NAME = "embed";

    private final AuditLoggingFacade auditLoggingFacade;

    public OperationsApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void logGetOperations(String username, String spaceId, String mtaId) {
        String performedAction = MessageFormat.format(Messages.LIST_OPERATIONS_AUDIT_LOG_MESSAGE, spaceId);
        Map<String, String> configIdentifiers = createAuditLogGetOperationsConfigurationIdentifier(mtaId);
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           spaceId,
                                                                           performedAction,
                                                                           Messages.OPERATION_LIST_AUDIT_LOG_CONFIG,
                                                                           configIdentifiers));
    }

    public void logGetOperationActions(String username, String spaceId, String operationId) {
        String performedAction = MessageFormat.format(Messages.LIST_OPERATION_ACTIONS_AUDIT_LOG_MESSAGE, spaceId);
        Map<String, String> configIdentifiers = createAuditLogGetOperationLogsConfigurationIdentifier(operationId);
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           spaceId,
                                                                           performedAction,
                                                                           Messages.OPERATION_ACTIONS_LIST_AUDIT_LOG_CONFIG,
                                                                           configIdentifiers));
    }

    public void logExecuteOperationAction(String username, String spaceId, String operationId, String actionId) {
        String performedAction = MessageFormat.format(Messages.EXECUTE_OPERATION_AUDIT_LOG_MESSAGE, spaceId);
        Map<String, String> configIdentifiers = createAuditLogExecuteOperationActionConfigurationIdentifier(operationId, actionId);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username,
                                                                                    spaceId,
                                                                                    performedAction,
                                                                                    Messages.EXECUTE_OPERATION_AUDIT_LOG_CONFIG,
                                                                                    configIdentifiers),
                                                          ConfigurationChangeActions.CONFIGURATION_CREATE);
    }

    public void logGetOperationLogs(String username, String spaceId, String operationId) {
        String performedAction = MessageFormat.format(Messages.GET_OPERATION_LOGS_AUDIT_LOG_MESSAGE, spaceId);
        Map<String, String> configIdentifiers = createAuditLogGetOperationLogsConfigurationIdentifier(operationId);
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           spaceId,
                                                                           performedAction,
                                                                           Messages.LIST_OPERATION_LOGS_AUDIT_LOG_CONFIG,
                                                                           configIdentifiers));
    }

    public void logGetOperationLogContent(String username, String spaceId, String operationId, String logId) {
        String performedAction = MessageFormat.format(Messages.GET_OPERATION_LOG_CONTENT_AUDIT_LOG_MESSAGE, spaceId);
        Map<String, String> configIdentifiers = createAuditLogGetOperationLogContentConfigurationIdentifier(operationId, logId);
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           spaceId,
                                                                           performedAction,
                                                                           Messages.GET_OPERATION_LOG_CONTENT_AUDIT_LOG_CONFIG,
                                                                           configIdentifiers));
    }

    public void logStartOperation(String username, String spaceId, Operation operation) {
        String performedAction = MessageFormat.format(Messages.START_OPERATION_AUDIT_LOG_MESSAGE, operation.getProcessType()
                                                                                                           .getName(),
                                                      spaceId);
        Map<String, String> configIdentifiers = createAuditLogStartOperationConfigurationIdentifier(operation);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username,
                                                                                    spaceId,
                                                                                    performedAction,
                                                                                    Messages.START_OPERATION_AUDIT_LOG_CONFIG,
                                                                                    configIdentifiers),
                                                          ConfigurationChangeActions.CONFIGURATION_CREATE);
    }

    public void logGetOperation(String username, String spaceId, String operationId, String embed) {
        String performedAction = MessageFormat.format(Messages.GET_INFO_FOR_OPERATION, spaceId);
        Map<String, String> configIdentifiers = createAuditLogGetOperationConfigurationIdentifier(operationId, embed);
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           spaceId,
                                                                           performedAction,
                                                                           Messages.GET_OPERATION_INFO_AUDIT_LOG_CONFIG,
                                                                           configIdentifiers));
    }

    private Map<String, String> createAuditLogGetOperationsConfigurationIdentifier(String mtaId) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(MTA_ID_PROPERTY_NAME, mtaId);

        return identifiers;
    }

    private Map<String, String> createAuditLogGetOperationConfigurationIdentifier(String operationId, String embed) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(OPERATION_ID_PROPERTY_NAME, operationId);
        identifiers.put(EMBED_PROPERTY_NAME, embed);

        return identifiers;
    }

    private Map<String, String> createAuditLogExecuteOperationActionConfigurationIdentifier(String operationId, String actionId) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(ACTION_ID_PROPERTY_NAME, actionId);
        identifiers.put(OPERATION_ID_PROPERTY_NAME, operationId);

        return identifiers;
    }

    private Map<String, String> createAuditLogGetOperationLogsConfigurationIdentifier(String operationId) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(OPERATION_ID_PROPERTY_NAME, operationId);

        return identifiers;
    }

    private Map<String, String> createAuditLogGetOperationLogContentConfigurationIdentifier(String operationId, String logId) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(OPERATION_ID_PROPERTY_NAME, operationId);
        identifiers.put(LOG_ID_PROPERTY_NAME, logId);

        return identifiers;
    }

    private Map<String, String> createAuditLogStartOperationConfigurationIdentifier(Operation operation) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(PROCESS_TYPE_PROPERTY_NAME, operation.getProcessType()
                                                             .getName());
        identifiers.put(PROCESS_ID_PROPERTY_NAME, operation.getProcessId());
        identifiers.put(MTA_ID_PROPERTY_NAME, operation.getMtaId());

        return identifiers;
    }
}
