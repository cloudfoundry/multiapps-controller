package org.cloudfoundry.multiapps.controller.persistence.services;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.List;

class ProcessLoggerProviderTest {

    private final String TEST_LOG_NAME = "test-log-name";
    private final String TEST_LOG_NAME_WITH_EXTENSION = TEST_LOG_NAME + ".log";
    private final String DEFAULT_LOG_NAME = "OPERATION.log";
    private final DelegateExecution delegateExecution = Mockito.mock(DelegateExecution.class);
    private final String TEST_CORRELATION_ID = "test-correlation-id";
    private final String TEST_SECOND_CORRELATION_ID = "test-second-correlation-id";
    private final String TEST_TASK_ID = "test-task-id";
    private final String TEST_SPACE_ID = "test-space-id";
    private final org.apache.logging.log4j.core.LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
    private final PatternLayout patternLayout = PatternLayout.newBuilder()
                                                             .withPattern(EMPTY)
                                                             .withConfiguration(loggerContext.getConfiguration())
                                                             .build();

    @Spy
    private ProcessLoggerProvider processLoggerProvider;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(delegateExecution.getVariable(Constants.CORRELATION_ID)).thenReturn(TEST_CORRELATION_ID);
        when(delegateExecution.getVariable(Constants.TASK_ID)).thenReturn(TEST_TASK_ID);
        when(delegateExecution.getCurrentActivityId()).thenReturn(TEST_TASK_ID);
        when(delegateExecution.getVariable(Constants.VARIABLE_NAME_SPACE_ID)).thenReturn(TEST_SPACE_ID);
        when(delegateExecution.getProcessInstanceId()).thenReturn(TEST_TASK_ID);
    }

    @Test
    void testCorrectlyCreatedProcessLoggerWithProvidedPatternLayoutAndLogName() {
        ProcessLogger processLogger = processLoggerProvider.getLogger(delegateExecution, TEST_LOG_NAME, A -> patternLayout);
        Assertions.assertEquals(processLogger.getActivityId(), TEST_TASK_ID);
        Assertions.assertEquals(processLogger.getOperationLogEntry().getOperationLogName(), TEST_LOG_NAME_WITH_EXTENSION);
        Assertions.assertEquals(processLogger.getOperationLogEntry().getSpace(), TEST_SPACE_ID);
        Assertions.assertEquals(processLogger.getOperationLogEntry().getOperationId(), TEST_CORRELATION_ID);
    }

    @Test
    void testCorrectlyCreatedProcessLoggerWithLogName() {
        ProcessLogger processLogger = processLoggerProvider.getLogger(delegateExecution, TEST_LOG_NAME);
        Assertions.assertEquals(processLogger.getActivityId(), TEST_TASK_ID);
        Assertions.assertEquals(processLogger.getOperationLogEntry().getOperationLogName(), TEST_LOG_NAME_WITH_EXTENSION);
        Assertions.assertEquals(processLogger.getOperationLogEntry().getSpace(), TEST_SPACE_ID);
        Assertions.assertEquals(processLogger.getOperationLogEntry().getOperationId(), TEST_CORRELATION_ID);
    }

    @Test
    void testCorrectlyCreatedProcessLogger() {
        ProcessLogger processLogger = processLoggerProvider.getLogger(delegateExecution);
        Assertions.assertEquals(processLogger.getActivityId(), TEST_TASK_ID);
        Assertions.assertEquals(processLogger.getOperationLogEntry().getOperationLogName(), DEFAULT_LOG_NAME);
        Assertions.assertEquals(processLogger.getOperationLogEntry().getSpace(), TEST_SPACE_ID);
        Assertions.assertEquals(processLogger.getOperationLogEntry().getOperationId(), TEST_CORRELATION_ID);
    }

    @Test
    void testExistingLoggers() {
        processLoggerProvider.getLogger(delegateExecution);

        when(delegateExecution.getVariable(Constants.CORRELATION_ID)).thenReturn(TEST_SECOND_CORRELATION_ID);
        processLoggerProvider.getLogger(delegateExecution);

        List<ProcessLogger> processLoggers = processLoggerProvider.getExistingLoggers(TEST_CORRELATION_ID, TEST_TASK_ID);
        Assertions.assertEquals(processLoggers.size(), 1);
    }

    @Test
    void testRemoveExistingLogger() {
        processLoggerProvider.getLogger(delegateExecution);

        List<ProcessLogger> processLoggers = processLoggerProvider.getExistingLoggers(TEST_CORRELATION_ID, TEST_TASK_ID);
        Assertions.assertEquals(processLoggers.size(), 1);

        processLoggerProvider.removeProcessLoggerFromCache(processLoggers.get(0));

        Assertions.assertEquals(processLoggerProvider.getExistingLoggers(TEST_CORRELATION_ID, TEST_TASK_ID).size(), 0);
    }
}
