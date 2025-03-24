package org.cloudfoundry.multiapps.controller.persistence.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.test.TestDataSourceProvider;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

class ProcessLoggerPersisterTest {

    private final static String TEST_CORRELATION_ID = "test-correlation-id";
    private final static String TEST_LOG_NAME = "test-log-name";
    private final static String TEST_TASK_ID = "test-task-id";
    private final static String TEST_SPACE_ID = "test-space-id";

    @Mock
    private DelegateExecution delegateExecution;
    @Mock
    private ProcessLogsPersistenceService processLogsPersistenceService;

    @Spy
    private ProcessLoggerProvider processLoggerProvider;

    private ProcessLoggerPersister processLoggerPersister;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(delegateExecution.getVariable(Constants.CORRELATION_ID)).thenReturn(TEST_CORRELATION_ID);
        when(delegateExecution.getVariable(Constants.TASK_ID)).thenReturn(TEST_TASK_ID);
        when(delegateExecution.getCurrentActivityId()).thenReturn(TEST_TASK_ID);
        when(delegateExecution.getVariable(Constants.VARIABLE_NAME_SPACE_ID)).thenReturn(TEST_SPACE_ID);
        when(delegateExecution.getProcessInstanceId()).thenReturn(TEST_TASK_ID);
        processLoggerPersister = new ProcessLoggerPersister(processLoggerProvider, processLogsPersistenceService);
    }

    @Test
    void testPersistLog() {
        ProcessLogger processLogger = processLoggerProvider.getLogger(delegateExecution);
        ProcessLogger processLoggerSecond = processLoggerProvider.getLogger(delegateExecution, TEST_LOG_NAME);

        processLoggerPersister.persistLogs(TEST_CORRELATION_ID, TEST_TASK_ID);

        Mockito.verify(processLoggerProvider)
               .getExistingLoggers(TEST_CORRELATION_ID, TEST_TASK_ID);
        Mockito.verify(processLoggerProvider)
               .removeProcessLoggerFromCache(processLogger);
        Mockito.verify(processLoggerProvider)
               .removeProcessLoggerFromCache(processLoggerSecond);
        Mockito.verify(processLogsPersistenceService, times(2))
               .persistLog(any());

        Assertions.assertEquals(processLoggerProvider.getExistingLoggers(TEST_CORRELATION_ID, TEST_TASK_ID)
                                                     .size(),
                                0);
    }

    @Test
    void testPersistLogWithTwoLogsWithTheSameOperationLogName() {
        ProcessLogger processLogger = processLoggerProvider.getLogger(delegateExecution);
        ProcessLogger processLoggerSecond = processLoggerProvider.getLogger(delegateExecution, TEST_LOG_NAME);
        ProcessLogger processLoggerThird = processLoggerProvider.getLogger(delegateExecution, TEST_LOG_NAME);

        processLoggerPersister.persistLogs(TEST_CORRELATION_ID, TEST_TASK_ID);

        Mockito.verify(processLoggerProvider)
               .getExistingLoggers(TEST_CORRELATION_ID, TEST_TASK_ID);
        Mockito.verify(processLoggerProvider)
               .removeProcessLoggerFromCache(processLogger);
        Mockito.verify(processLoggerProvider)
               .removeProcessLoggerFromCache(processLoggerSecond);
        Mockito.verify(processLoggerProvider)
               .removeProcessLoggerFromCache(processLoggerThird);
        Mockito.verify(processLogsPersistenceService, times(2))
               .persistLog(any());

        Assertions.assertEquals(processLoggerProvider.getExistingLoggers(TEST_CORRELATION_ID, TEST_TASK_ID)
                                                     .size(),
                                0);
    }

    @Test
    void testPersistLogWithoutLogs() {
        processLoggerPersister.persistLogs(TEST_CORRELATION_ID, TEST_TASK_ID);

        Mockito.verify(processLoggerProvider)
               .getExistingLoggers(TEST_CORRELATION_ID, TEST_TASK_ID);
        Mockito.verify(processLogsPersistenceService, times(0))
               .persistLog(any());
    }
}
