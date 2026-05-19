package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class OperationsApiServiceAuditLogTest {

    private static final String USERNAME = "alice";
    private static final String SPACE_ID = "space-guid";
    private static final String OPERATION_ID = "op-1";
    private static final String MTA_ID = "my-mta";
    private static final String PROCESS_ID = "process-1";

    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private OperationsApiServiceAuditLog operationsApiServiceAuditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        operationsApiServiceAuditLog = new OperationsApiServiceAuditLog(auditLoggingFacade);
    }

    @Test
    void testLogGetOperationsLogsDataAccess() {
        operationsApiServiceAuditLog.logGetOperations(USERNAME, SPACE_ID, MTA_ID);

        AuditLogConfiguration captured = captureDataAccessConfig();
        Assertions.assertEquals(USERNAME, captured.getUserId());
        Assertions.assertEquals(SPACE_ID, captured.getSpaceId());
        Assertions.assertTrue(containsParameter(captured, "mtaId", MTA_ID));
    }

    @Test
    void testLogGetOperationActionsLogsDataAccessWithOperationId() {
        operationsApiServiceAuditLog.logGetOperationActions(USERNAME, SPACE_ID, OPERATION_ID);

        AuditLogConfiguration captured = captureDataAccessConfig();
        Assertions.assertEquals(USERNAME, captured.getUserId());
        Assertions.assertTrue(containsParameter(captured, "operationId", OPERATION_ID));
    }

    @Test
    void testLogExecuteOperationActionLogsConfigurationCreate() {
        operationsApiServiceAuditLog.logExecuteOperationAction(USERNAME, SPACE_ID, OPERATION_ID, "abort");

        ArgumentCaptor<AuditLogConfiguration> configCaptor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        ArgumentCaptor<ConfigurationChangeActions> actionCaptor = ArgumentCaptor.forClass(ConfigurationChangeActions.class);
        Mockito.verify(auditLoggingFacade)
               .logConfigurationChangeAuditLog(configCaptor.capture(), actionCaptor.capture());
        Assertions.assertEquals(ConfigurationChangeActions.CONFIGURATION_CREATE, actionCaptor.getValue());
        Assertions.assertTrue(containsParameter(configCaptor.getValue(), "actionId", "abort"));
        Assertions.assertTrue(containsParameter(configCaptor.getValue(), "operationId", OPERATION_ID));
    }

    @Test
    void testLogGetOperationLogsLogsDataAccess() {
        operationsApiServiceAuditLog.logGetOperationLogs(USERNAME, SPACE_ID, OPERATION_ID);

        AuditLogConfiguration captured = captureDataAccessConfig();
        Assertions.assertTrue(containsParameter(captured, "operationId", OPERATION_ID));
    }

    @Test
    void testLogGetOperationLogContentLogsDataAccessWithLogId() {
        operationsApiServiceAuditLog.logGetOperationLogContent(USERNAME, SPACE_ID, OPERATION_ID, "log-1");

        AuditLogConfiguration captured = captureDataAccessConfig();
        Assertions.assertTrue(containsParameter(captured, "operationId", OPERATION_ID));
        Assertions.assertTrue(containsParameter(captured, "logId", "log-1"));
    }

    @Test
    void testLogStartOperationLogsConfigurationCreateWithProcessDetails() {
        Operation operation = ImmutableOperation.builder()
                                                .processId(PROCESS_ID)
                                                .processType(ProcessType.DEPLOY)
                                                .mtaId(MTA_ID)
                                                .build();

        operationsApiServiceAuditLog.logStartOperation(USERNAME, SPACE_ID, operation);

        ArgumentCaptor<AuditLogConfiguration> configCaptor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logConfigurationChangeAuditLog(configCaptor.capture(), Mockito.eq(ConfigurationChangeActions.CONFIGURATION_CREATE));
        Assertions.assertTrue(containsParameter(configCaptor.getValue(), "processId", PROCESS_ID));
        Assertions.assertTrue(containsParameter(configCaptor.getValue(), "mtaId", MTA_ID));
        Assertions.assertTrue(containsParameter(configCaptor.getValue(), "processType", ProcessType.DEPLOY.getName()));
    }

    @Test
    void testLogGetOperationLogsDataAccessWithEmbed() {
        operationsApiServiceAuditLog.logGetOperation(USERNAME, SPACE_ID, OPERATION_ID, "messages");

        AuditLogConfiguration captured = captureDataAccessConfig();
        Assertions.assertTrue(containsParameter(captured, "operationId", OPERATION_ID));
        Assertions.assertTrue(containsParameter(captured, "embed", "messages"));
    }

    private AuditLogConfiguration captureDataAccessConfig() {
        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logDataAccessAuditLog(captor.capture());
        return captor.getValue();
    }

    private boolean containsParameter(AuditLogConfiguration configuration, String key, String value) {
        return configuration.getConfigurationIdentifiers()
                            .stream()
                            .anyMatch(identifier -> key.equals(identifier.getIdentifierName())
                                && value.equals(identifier.getIdentifierValue()));
    }

}
