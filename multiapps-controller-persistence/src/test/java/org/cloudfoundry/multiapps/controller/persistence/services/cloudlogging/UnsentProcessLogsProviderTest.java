package org.cloudfoundry.multiapps.controller.persistence.services.cloudlogging;

import java.util.List;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class UnsentProcessLogsProviderTest {

    private static final String OPERATION_ID = "op-123";
    private static final String SPACE_ID = "space-1";

    @Mock
    private ProcessLogsPersistenceService processLogsPersistenceService;

    private UnsentProcessLogsProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        provider = new UnsentProcessLogsProvider(processLogsPersistenceService);
    }

    @Test
    void getUnsentProcessLogs_returnsLogsFromService() throws FileStorageException {
        OperationLogEntry entry = buildEntry();
        when(processLogsPersistenceService.listOperationLogsBySpaceAndOperationId(SPACE_ID, OPERATION_ID)).thenReturn(List.of(entry));

        List<OperationLogEntry> result = provider.getUnsentProcessLogs(buildConfig(true));

        assertEquals(1, result.size());
        assertEquals(entry, result.get(0));
    }

    @Test
    void getUnsentProcessLogs_failSafeTrueReturnsEmptyListOnStorageException() throws FileStorageException {
        when(processLogsPersistenceService.listOperationLogsBySpaceAndOperationId(anyString(), anyString())).thenThrow(
            new FileStorageException("db error"));

        List<OperationLogEntry> result = provider.getUnsentProcessLogs(buildConfig(true));

        assertTrue(result.isEmpty());
    }

    @Test
    void getUnsentProcessLogs_failSafeFalseThrowsOnStorageException() throws FileStorageException {
        when(processLogsPersistenceService.listOperationLogsBySpaceAndOperationId(anyString(), anyString())).thenThrow(
            new FileStorageException("db error"));

        assertThrows(SLException.class, () -> provider.getUnsentProcessLogs(buildConfig(false)));
    }

    private static LoggingConfiguration buildConfig(boolean failSafe) {
        return ImmutableLoggingConfiguration.builder()
                                            .operationId(OPERATION_ID)
                                            .mtaSpaceId(SPACE_ID)
                                            .logLevel(LogLevel.INFO)
                                            .isFailSafe(failSafe)
                                            .endpointUrl("https://cls.example.com")
                                            .serverCa("server-ca")
                                            .clientCert("client-cert")
                                            .clientKey("client-key")
                                            .build();
    }

    private static OperationLogEntry buildEntry() {
        return ImmutableOperationLogEntry.builder()
                                         .operationId(OPERATION_ID)
                                         .operationLog("log")
                                         .operationLogName("test-log")
                                         .build();
    }
}
